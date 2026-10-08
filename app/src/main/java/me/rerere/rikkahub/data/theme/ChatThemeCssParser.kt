/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

/**
 * 聊天主题 CSS 解析器。
 *
 * 用 CSS 语法书写样式，解析成 [ChatThemeSpec]。这不是完整的 CSS 引擎：
 * 只认识下面列出的选择器与属性，其余一律静默忽略并记入
 * [ChatThemeParseResult.unknownProperties]，方便用户知道哪些写了不生效。
 *
 * 设计约束（对应需求）：
 * - 任何输入都不会抛异常
 * - 回退粒度：只丢弃出错的那一条声明，同块内其余声明照常生效；
 *   仅当解析器自身抛异常（文件损坏）时才整体回退 [ChatThemeSpec.BUILTIN]
 * - 所有问题都带行号，便于界面提示"哪一行有问题"
 *
 * 支持的选择器：
 * ```
 * .bubble                两侧气泡
 * .bubble.user           用户气泡（亦可写 .user-bubble）
 * .bubble.assistant      助手气泡（亦可写 .assistant-bubble）
 * .avatar                两侧头像
 * .avatar.user           用户头像（亦可写 .user-avatar）
 * .avatar.assistant      助手头像（亦可写 .assistant-avatar）
 * .message-list          消息列表布局（亦可写 .chat），消息间距属性名为 gap
 *                        （别名 spacing / row-gap / margin / margin-bottom / message-spacing）
 * .voice                 语音气泡（亦可写 .voice-bubble）
 * ```
 */
object ChatThemeCssParser {

    fun parse(css: String, name: String = ""): ChatThemeParseResult {
        val ctx = ParseContext()
        var spec = ChatThemeSpec.BUILTIN.copy(name = name)
        try {
            for (block in tokenize(css, ctx)) {
                for (selector in block.selectors) {
                    val targets = resolveSelector(selector)
                    if (targets.isEmpty()) {
                        ctx.warn(block.line, "未知选择器 \"$selector\"")
                        continue
                    }
                    spec = applyBlock(spec, targets, block.declarations, ctx)
                }
            }
        } catch (e: Throwable) {
            // 兜底：解析器自身出意外（文件损坏等）时整体回退内置默认样式，
            // 避免留下一半新一半旧的错乱外观。值写错只丢弃那一条声明，不走这里。
            spec = ChatThemeSpec.BUILTIN.copy(name = name)
            ctx.error(0, "解析中断，已回退内置默认样式: ${e.message ?: e::class.simpleName}")
        }
        return ChatThemeParseResult(
            spec = spec,
            errors = ctx.errors.toList(),
            unknownProperties = ctx.unknown.toList(),
        )
    }

    // region 词法分析

    private data class Decl(val property: String, val value: String, val line: Int)

    private data class Block(val selectors: List<String>, val declarations: List<Decl>, val line: Int)

    /**
     * 扫描出 `选择器 { 属性: 值; ... }` 结构。
     * 注释被替换为等量换行以保持行号准确。
     */
    private fun tokenize(raw: String, ctx: ParseContext): List<Block> {
        val css = stripComments(raw, ctx)
        val blocks = mutableListOf<Block>()
        var i = 0
        var line = 1
        val selectorBuf = StringBuilder()
        var selectorLine = 1

        while (i < css.length) {
            val c = css[i]
            when (c) {
                '\n' -> {
                    line++
                    selectorBuf.append(' ')
                    i++
                }

                '{' -> {
                    val selectorText = selectorBuf.toString().trim()
                    selectorBuf.setLength(0)
                    val bodyStart = i + 1
                    var j = bodyStart
                    // 不支持嵌套：遇到 '}' 正常闭合；遇到下一个 '{' 说明本块漏写了 '}'
                    while (j < css.length && css[j] != '}' && css[j] != '{') j++
                    val hitNextBlock = j < css.length && css[j] == '{'
                    // 漏写 '}' 时，把尾部那段文字还给下一个选择器，
                    // 否则 ".bubble.assistant { background-color" 会被当成属性名
                    val bodyEnd = if (hitNextBlock) {
                        maxOf(
                            css.lastIndexOf(';', j - 1) + 1,
                            css.lastIndexOf('\n', j - 1) + 1,
                            bodyStart,
                        )
                    } else {
                        minOf(j, css.length)
                    }
                    if (hitNextBlock || j >= css.length) {
                        val who = if (selectorText.isEmpty()) "该规则" else "\"$selectorText\""
                        ctx.error(selectorLine, "$who 的块缺少闭合的 \"}\"")
                    }
                    val body = css.substring(bodyStart, bodyEnd)
                    val declarations = parseDeclarations(body, line, ctx)
                    val selectors = selectorText.split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    if (selectors.isEmpty()) {
                        ctx.error(selectorLine, "缺少选择器")
                    } else {
                        blocks += Block(selectors, declarations, selectorLine)
                    }
                    // 回退到 bodyEnd 时，剩下的文字由主循环重新当作选择器累积
                    val resumeAt = if (bodyEnd < j) bodyEnd else minOf(j + 1, css.length)
                    line += css.substring(bodyStart, minOf(resumeAt, css.length)).count { it == '\n' }
                    i = resumeAt
                    selectorLine = line
                }

                '}' -> {
                    ctx.error(line, "多余的 \"}\"")
                    i++
                    selectorLine = line
                }

                else -> {
                    if (selectorBuf.isBlank() && !c.isWhitespace()) selectorLine = line
                    selectorBuf.append(c)
                    i++
                }
            }
        }
        if (selectorBuf.isNotBlank()) {
            ctx.error(selectorLine, "\"${selectorBuf.toString().trim()}\" 后面缺少 \"{ }\"")
        }
        return blocks
    }

    /** 把注释换成等量换行，保证后续行号不偏移；注释没闭合时报错 */
    private fun stripComments(css: String, ctx: ParseContext): String {
        if (!css.contains("/*")) return css
        val sb = StringBuilder(css.length)
        var i = 0
        var line = 1
        while (i < css.length) {
            if (i + 1 < css.length && css[i] == '/' && css[i + 1] == '*') {
                val end = css.indexOf("*/", i + 2)
                if (end < 0) ctx.error(line, "注释 \"/*\" 缺少闭合的 \"*/\"")
                val stop = if (end < 0) css.length else end + 2
                for (k in i until stop) if (css[k] == '\n') {
                    sb.append('\n')
                    line++
                }
                i = stop
            } else {
                if (css[i] == '\n') line++
                sb.append(css[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun parseDeclarations(body: String, startLine: Int, ctx: ParseContext): List<Decl> {
        val out = mutableListOf<Decl>()
        var line = startLine
        for (piece in splitTopLevel(body, ';')) {
            val lineOfPiece = line
            line += piece.count { it == '\n' }
            val text = piece.trim()
            if (text.isEmpty()) continue
            // 行号指向属性名真正出现的那一行
            val leading = piece.takeWhile { it.isWhitespace() }.count { it == '\n' }
            val declLine = lineOfPiece + leading
            val colon = text.indexOf(':')
            if (colon <= 0) {
                ctx.error(declLine, "\"$text\" 不是合法声明，应为 \"属性: 值;\"")
                continue
            }
            val property = text.substring(0, colon).trim().lowercase()
            val value = text.substring(colon + 1).trim().removeSuffix("!important").trim()
            if (property.isEmpty()) {
                ctx.error(declLine, "缺少属性名")
                continue
            }
            if (value.isEmpty()) {
                ctx.error(declLine, "属性 \"$property\" 缺少值")
                continue
            }
            out += Decl(property, value, declLine)
        }
        return out
    }

    /** 按分隔符切分，但跳过括号内的分隔符 */
    private fun splitTopLevel(text: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        val sb = StringBuilder()
        for (c in text) {
            when {
                c == '(' -> { depth++; sb.append(c) }
                c == ')' -> { if (depth > 0) depth--; sb.append(c) }
                c == delimiter && depth == 0 -> { out += sb.toString(); sb.setLength(0) }
                else -> sb.append(c)
            }
        }
        out += sb.toString()
        return out
    }

    // endregion

    // region 选择器

    private enum class Target {
        USER_BUBBLE, ASSISTANT_BUBBLE, USER_AVATAR, ASSISTANT_AVATAR, MESSAGE, VOICE
    }

    /** 把类选择器映射到作用目标；无法识别时返回空列表 */
    private fun resolveSelector(raw: String): List<Target> {
        val s = raw.trim().lowercase()
        if (s.length < 2 || !s.startsWith('.')) return emptyList()
        // 只支持单个元素上的类选择器组合，如 .bubble.user
        if (s.any { it.isWhitespace() || it in ">+~#[]:*" }) return emptyList()

        val canonical = mutableSetOf<String>()
        for (cls in s.removePrefix(".").split('.')) {
            if (cls.isEmpty()) return emptyList()
            when (cls) {
                "bubble", "avatar", "user", "assistant" -> canonical += cls
                "user-bubble", "bubble-user", "me", "self" -> canonical += listOf("bubble", "user")
                "assistant-bubble", "bubble-assistant", "ai", "other" ->
                    canonical += listOf("bubble", "assistant")

                "user-avatar", "avatar-user" -> canonical += listOf("avatar", "user")
                "assistant-avatar", "avatar-assistant" -> canonical += listOf("avatar", "assistant")
                "message-list", "messages", "chat", "chat-list" -> canonical += "message-list"
                "voice", "voice-bubble", "audio", "audio-bubble" -> canonical += "voice"
                else -> return emptyList()
            }
        }
        val user = "user" in canonical
        val assistant = "assistant" in canonical
        return when {
            "message-list" in canonical -> listOf(Target.MESSAGE)
            "voice" in canonical -> listOf(Target.VOICE)
            "bubble" in canonical -> when {
                user && !assistant -> listOf(Target.USER_BUBBLE)
                assistant && !user -> listOf(Target.ASSISTANT_BUBBLE)
                else -> listOf(Target.USER_BUBBLE, Target.ASSISTANT_BUBBLE)
            }

            "avatar" in canonical -> when {
                user && !assistant -> listOf(Target.USER_AVATAR)
                assistant && !user -> listOf(Target.ASSISTANT_AVATAR)
                else -> listOf(Target.USER_AVATAR, Target.ASSISTANT_AVATAR)
            }

            else -> emptyList()
        }
    }

    private fun applyBlock(
        spec: ChatThemeSpec,
        targets: List<Target>,
        declarations: List<Decl>,
        ctx: ParseContext,
    ): ChatThemeSpec {
        var out = spec
        targets.forEachIndexed { index, target ->
            // 一个选择器命中多个目标时只报一次问题，避免重复提示
            val report = index == 0
            out = when (target) {
                Target.USER_BUBBLE ->
                    out.copy(userBubble = applyBubble(out.userBubble, declarations, ctx, report))

                Target.ASSISTANT_BUBBLE ->
                    out.copy(assistantBubble = applyBubble(out.assistantBubble, declarations, ctx, report))

                Target.USER_AVATAR ->
                    out.copy(userAvatar = applyAvatar(out.userAvatar, declarations, ctx, report))

                Target.ASSISTANT_AVATAR ->
                    out.copy(assistantAvatar = applyAvatar(out.assistantAvatar, declarations, ctx, report))

                Target.MESSAGE ->
                    out.copy(message = applyMessage(out.message, declarations, ctx, report))

                Target.VOICE ->
                    out.copy(voice = applyVoice(out.voice, declarations, ctx, report))
            }
        }
        return out
    }

    // endregion

    // region 取值

    /** 长度：支持 12、12dp、12px、0 */
    private fun length(value: String): Float? {
        val v = value.trim().lowercase().removeSuffix("dp").removeSuffix("px").trim()
        val f = v.toFloatOrNull() ?: return null
        return if (f.isFinite() && f >= 0f) f else null
    }

    /** 允许负值的长度，供偏移量使用 */
    private fun offset(value: String): Float? {
        val v = value.trim().lowercase().removeSuffix("dp").removeSuffix("px").trim()
        return v.toFloatOrNull()?.takeIf { it.isFinite() }
    }

    /** 百分比：支持 70%、0.7 —— 统一返回 0f~1f */
    private fun percent(value: String): Float? {
        val v = value.trim()
        val f = if (v.endsWith("%")) {
            v.dropLast(1).trim().toFloatOrNull()?.div(100f)
        } else {
            v.toFloatOrNull()
        } ?: return null
        return if (f.isFinite() && f > 0f && f <= 1f) f else null
    }

    private fun bool(value: String): Boolean? = when (value.trim().lowercase()) {
        "true", "yes", "on", "1", "visible", "show", "block" -> true
        "false", "no", "off", "0", "hidden", "hide", "none" -> false
        else -> null
    }

    private fun int(value: String): Int? =
        value.trim().toIntOrNull()?.takeIf { it > 0 }

    /** 颜色：#rgb / #rgba / #rrggbb / #rrggbbaa / rgb() / rgba() / transparent / 常用色名 */
    private fun color(value: String): Long? {
        val v = value.trim().lowercase()
        if (v == "transparent" || v == "none") return 0x00000000L
        NAMED_COLORS[v]?.let { return it }
        if (v.startsWith("#")) {
            val hex = v.removePrefix("#")
            if (hex.any { it !in "0123456789abcdef" }) return null
            val expanded = when (hex.length) {
                3 -> "ff" + hex.map { "$it$it" }.joinToString("")
                4 -> hex[3].let { "$it$it" } + hex.take(3).map { "$it$it" }.joinToString("")
                6 -> "ff$hex"
                8 -> hex.substring(6, 8) + hex.substring(0, 6)
                else -> return null
            }
            return expanded.toLongOrNull(16)
        }
        if (v.startsWith("rgb")) {
            val inner = v.substringAfter('(', "").substringBefore(')', "")
            if (inner.isEmpty()) return null
            val parts = inner.split(',', ' ', '/').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size != 3 && parts.size != 4) return null
            val rgb = parts.take(3).map {
                val n = if (it.endsWith("%")) {
                    it.dropLast(1).toFloatOrNull()?.div(100f)?.times(255f)
                } else {
                    it.toFloatOrNull()
                } ?: return null
                n.toInt().coerceIn(0, 255).toLong()
            }
            val alpha = if (parts.size == 4) {
                val a = parts[3].let {
                    if (it.endsWith("%")) it.dropLast(1).toFloatOrNull()?.div(100f) else it.toFloatOrNull()
                } ?: return null
                (a * 255f).toInt().coerceIn(0, 255).toLong()
            } else 255L
            return (alpha shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]
        }
        return null
    }

    private val NAMED_COLORS = mapOf(
        "black" to 0xFF000000L, "white" to 0xFFFFFFFFL, "red" to 0xFFFF0000L,
        "green" to 0xFF008000L, "blue" to 0xFF0000FFL, "yellow" to 0xFFFFFF00L,
        "orange" to 0xFFFFA500L, "purple" to 0xFF800080L, "pink" to 0xFFFFC0CBL,
        "gray" to 0xFF808080L, "grey" to 0xFF808080L, "lightgray" to 0xFFD3D3D3L,
        "lightgrey" to 0xFFD3D3D3L, "darkgray" to 0xFFA9A9A9L, "darkgrey" to 0xFFA9A9A9L,
        "cyan" to 0xFF00FFFFL, "magenta" to 0xFFFF00FFL, "brown" to 0xFFA52A2AL,
        "gold" to 0xFFFFD700L, "silver" to 0xFFC0C0C0L, "navy" to 0xFF000080L,
        "teal" to 0xFF008080L, "olive" to 0xFF808000L, "lime" to 0xFF00FF00L,
        "indigo" to 0xFF4B0082L, "violet" to 0xFFEE82EEL, "skyblue" to 0xFF87CEEBL,
        "lightblue" to 0xFFADD8E6L, "lightgreen" to 0xFF90EE90L, "beige" to 0xFFF5F5DCL,
        "ivory" to 0xFFFFFFF0L, "khaki" to 0xFFF0E68CL, "salmon" to 0xFFFA8072L,
        "coral" to 0xFFFF7F50L, "tomato" to 0xFFFF6347L, "orchid" to 0xFFDA70D6L,
        "plum" to 0xFFDDA0DDL, "wheat" to 0xFFF5DEB3L, "snow" to 0xFFFFFAFAL,
    )

    /**
     * border 简写：`1dp solid #ccc` / `1px #ccc` / `none`
     * 返回 宽度 to 颜色（颜色可为 null 表示未指定）
     */
    private fun border(value: String): Pair<Float, Long?>? {
        val v = value.trim().lowercase()
        if (v == "none" || v == "0") return 0f to null
        var width: Float? = null
        var c: Long? = null
        for (token in v.split(' ', '\t').map { it.trim() }.filter { it.isNotEmpty() }) {
            when {
                token in BORDER_STYLES -> Unit
                length(token) != null && width == null -> width = length(token)
                color(token) != null && c == null -> c = color(token)
                else -> return null
            }
        }
        return (width ?: return null) to c
    }

    private val BORDER_STYLES = setOf("solid", "dashed", "dotted", "double", "groove", "ridge")

    /**
     * 单个圆角值：支持 12dp 与 50%。返回 数值 to 是否为百分比。
     * 百分比上限 50%，到 50% 即为全圆/胶囊。
     */
    private fun radiusLength(value: String): Pair<Float, Boolean>? {
        val v = value.trim().lowercase()
        if (v.endsWith("%")) {
            val f = v.dropLast(1).trim().toFloatOrNull() ?: return null
            return if (f.isFinite() && f >= 0f) f.coerceAtMost(50f) to true else null
        }
        return length(v)?.let { it to false }
    }

    /**
     * border-radius 简写，遵循 CSS 的 1/2/3/4 值规则。
     * 返回 (左上/右上/右下/左下) to 是否为百分比；
     * 混写 dp 与 % 时按百分比处理（单个气泡只能整体用一种单位）。
     */
    private fun radius(value: String): Pair<FloatArray, Boolean>? {
        val parts = value.trim().split(' ', '\t').map { it.trim() }.filter { it.isNotEmpty() }
        val pairs = parts.map { radiusLength(it) ?: return null }
        val percent = pairs.any { it.second }
        val nums = pairs.map { it.first }
        val arr = when (nums.size) {
            1 -> floatArrayOf(nums[0], nums[0], nums[0], nums[0])
            2 -> floatArrayOf(nums[0], nums[1], nums[0], nums[1])
            3 -> floatArrayOf(nums[0], nums[1], nums[2], nums[1])
            4 -> floatArrayOf(nums[0], nums[1], nums[2], nums[3])
            else -> return null
        }
        return arr to percent
    }

    /**
     * padding 简写。返回 纵向 to 横向。
     * 四值写法取 上/右（上下、左右不支持分开设定，故取其一）。
     */
    private fun padding(value: String): Pair<Float, Float>? {
        val parts = value.trim().split(' ', '\t').map { it.trim() }.filter { it.isNotEmpty() }
        val nums = parts.map { length(it) ?: return null }
        return when (nums.size) {
            1 -> nums[0] to nums[0]
            2, 3, 4 -> nums[0] to nums[1]
            else -> null
        }
    }

    // endregion

    // region 属性应用

    /**
     * 统一的声明处理骨架：[handler] 返回 true 表示认识该属性并已应用，
     * 返回 false 表示属性名不认识，由此处记入"未生效"清单；
     * 属性名认识但值非法时由 [handler] 内部调用 [ParseContext.error] 并返回 true。
     */
    private inline fun <T> applyAll(
        initial: T,
        declarations: List<Decl>,
        ctx: ParseContext,
        report: Boolean,
        handler: (T, Decl) -> T?,
    ): T {
        var acc = initial
        for (decl in declarations) {
            val next = handler(acc, decl)
            if (next == null) {
                if (report) ctx.warn(decl.line, "属性 \"${decl.property}\" 不支持，已忽略")
            } else {
                acc = next
            }
        }
        return acc
    }

    /** 值非法时统一提示，并保持原值不变 */
    private fun ParseContext.badValue(decl: Decl, expect: String) {
        error(decl.line, "属性 \"${decl.property}\" 的值 \"${decl.value}\" 无效，应为$expect")
    }

    private fun applyBubble(
        initial: BubbleStyle,
        declarations: List<Decl>,
        ctx: ParseContext,
        report: Boolean,
    ): BubbleStyle = applyAll(initial, declarations, ctx, report) { s, d ->
        when (d.property) {
            "border-radius" -> radius(d.value)?.let { (r, pct) ->
                s.copy(
                    radiusTopStart = r[0], radiusTopEnd = r[1],
                    radiusBottomEnd = r[2], radiusBottomStart = r[3],
                    radiusPercent = pct,
                )
            } ?: s.also { ctx.badValue(d, "1~4 个长度值或百分比") }

            "border-top-left-radius" ->
                radiusLength(d.value)?.let { (v, pct) -> s.copy(radiusTopStart = v, radiusPercent = pct) }
                    ?: s.also { ctx.badValue(d, "长度值或百分比") }

            "border-top-right-radius" ->
                radiusLength(d.value)?.let { (v, pct) -> s.copy(radiusTopEnd = v, radiusPercent = pct) }
                    ?: s.also { ctx.badValue(d, "长度值或百分比") }

            "border-bottom-right-radius" ->
                radiusLength(d.value)?.let { (v, pct) -> s.copy(radiusBottomEnd = v, radiusPercent = pct) }
                    ?: s.also { ctx.badValue(d, "长度值或百分比") }

            "border-bottom-left-radius" ->
                radiusLength(d.value)?.let { (v, pct) -> s.copy(radiusBottomStart = v, radiusPercent = pct) }
                    ?: s.also { ctx.badValue(d, "长度值或百分比") }

            "padding" -> padding(d.value)?.let {
                s.copy(paddingVertical = it.first, paddingHorizontal = it.second)
            } ?: s.also { ctx.badValue(d, "1~4 个长度值") }

            "padding-top", "padding-bottom", "padding-vertical" ->
                length(d.value)?.let { s.copy(paddingVertical = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "padding-left", "padding-right", "padding-horizontal" ->
                length(d.value)?.let { s.copy(paddingHorizontal = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "background", "background-color" ->
                color(d.value)?.let { s.copy(backgroundColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            "color", "text-color" ->
                color(d.value)?.let { s.copy(textColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            "max-width" ->
                percent(d.value)?.let { s.copy(maxWidthPercent = it) }
                    ?: s.also { ctx.badValue(d, "百分比，如 80%") }

            "border" -> border(d.value)?.let { (w, c) ->
                s.copy(borderWidth = w, borderColor = c ?: s.borderColor)
            } ?: s.also { ctx.badValue(d, "如 1dp solid #ccc") }

            "border-width" ->
                length(d.value)?.let { s.copy(borderWidth = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "border-color" ->
                color(d.value)?.let { s.copy(borderColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            "tail", "tail-visible" ->
                bool(d.value)?.let { s.copy(tailVisible = it) }
                    ?: s.also { ctx.badValue(d, "true 或 false") }

            "tail-size", "tail-width" ->
                length(d.value)?.let { s.copy(tailVisible = it > 0f, tailSize = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "tail-height", "tail-base" ->
                length(d.value)?.let { s.copy(tailHeight = if (it > 0f) it else null) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "tail-color", "tail-background" ->
                color(d.value)?.let { s.copy(backgroundColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            "tail-offset", "tail-top", "tail-margin" ->
                offset(d.value)?.let { s.copy(tailOffset = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "tail-position" -> parseTailPosition(d.value)?.let {
                s.copy(tailPosition = it, tailVisible = if (it == TailPosition.NONE) false else s.tailVisible)
            } ?: s.also { ctx.badValue(d, "auto/none/top-left/center-left/bottom-right 等") }

            "tail-shape" -> parseTailShape(d.value)?.let { s.copy(tailShape = it) }
                ?: s.also { ctx.badValue(d, "triangle 或 right-triangle") }

            "segment-spacing", "row-gap" ->
                length(d.value)?.let { s.copy(segmentSpacing = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            else -> null
        }
    }

    private fun parseTailPosition(value: String): TailPosition? =
        when (value.trim().lowercase().replace('_', '-')) {
            "auto" -> TailPosition.AUTO
            "none", "hidden", "off" -> TailPosition.NONE
            "top-left", "left-top" -> TailPosition.TOP_LEFT
            "top-right", "right-top" -> TailPosition.TOP_RIGHT
            "center-left", "left-center", "left", "middle-left" -> TailPosition.CENTER_LEFT
            "center-right", "right-center", "right", "middle-right" -> TailPosition.CENTER_RIGHT
            "bottom-left", "left-bottom" -> TailPosition.BOTTOM_LEFT
            "bottom-right", "right-bottom" -> TailPosition.BOTTOM_RIGHT
            else -> null
        }

    private fun parseTailShape(value: String): TailShape? =
        when (value.trim().lowercase().replace('_', '-')) {
            "triangle", "isosceles" -> TailShape.TRIANGLE
            "right-triangle", "right", "edge" -> TailShape.RIGHT_TRIANGLE
            else -> null
        }

    private fun applyAvatar(
        initial: AvatarStyle,
        declarations: List<Decl>,
        ctx: ParseContext,
        report: Boolean,
    ): AvatarStyle = applyAll(initial, declarations, ctx, report) { s, d ->
        when (d.property) {
            "size", "width", "height" ->
                length(d.value)?.let { s.copy(size = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "shape", "avatar-type" -> parseAvatarShape(d.value)?.let { s.copy(shape = it) }
                ?: s.also { ctx.badValue(d, "circle/rounded/square") }

            "border-radius" -> length(d.value)?.let {
                // 写了圆角却没写 shape 时，自动按圆角形状渲染
                s.copy(cornerRadius = it, shape = if (s.shape == AvatarShapeSpec.CIRCLE) AvatarShapeSpec.ROUNDED else s.shape)
            } ?: s.also { ctx.badValue(d, "长度值") }

            "gap", "margin", "margin-right", "margin-left" ->
                length(d.value)?.let { s.copy(gap = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "position", "avatar-position" -> parseAvatarPosition(d.value)?.let { s.copy(position = it) }
                ?: s.also { ctx.badValue(d, "top 或 side") }

            "display", "visible", "visibility" ->
                bool(d.value)?.let { s.copy(visible = it) }
                    ?: s.also { ctx.badValue(d, "true 或 false") }

            "border" -> border(d.value)?.let { (w, c) ->
                s.copy(borderWidth = w, borderColor = c ?: s.borderColor)
            } ?: s.also { ctx.badValue(d, "如 1dp solid #ccc") }

            "border-width" ->
                length(d.value)?.let { s.copy(borderWidth = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "border-color" ->
                color(d.value)?.let { s.copy(borderColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            else -> null
        }
    }

    private fun parseAvatarShape(value: String): AvatarShapeSpec? =
        when (value.trim().lowercase()) {
            "circle", "round", "circular" -> AvatarShapeSpec.CIRCLE
            "rounded", "rounded-square", "roundrect", "squircle" -> AvatarShapeSpec.ROUNDED
            "square", "rect", "rectangle" -> AvatarShapeSpec.SQUARE
            else -> null
        }

    private fun parseAvatarPosition(value: String): AvatarPositionSpec? =
        when (value.trim().lowercase()) {
            "top", "above", "header" -> AvatarPositionSpec.TOP
            "side", "left", "inline", "beside" -> AvatarPositionSpec.SIDE
            else -> null
        }

    private fun applyMessage(
        initial: MessageStyle,
        declarations: List<Decl>,
        ctx: ParseContext,
        report: Boolean,
    ): MessageStyle = applyAll(initial, declarations, ctx, report) { s, d ->
        when (d.property) {
            "gap", "spacing", "row-gap", "margin", "message-spacing",
            "margin-bottom", "margin-top", "margin-vertical", "bubble-spacing" ->
                length(d.value)?.let { s.copy(spacing = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "padding", "padding-left", "padding-right", "padding-horizontal" ->
                length(d.value)?.let { s.copy(horizontalPadding = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            else -> null
        }
    }

    private fun applyVoice(
        initial: VoiceStyle,
        declarations: List<Decl>,
        ctx: ParseContext,
        report: Boolean,
    ): VoiceStyle = applyAll(initial, declarations, ctx, report) { s, d ->
        when (d.property) {
            "border-radius" ->
                length(d.value)?.let { s.copy(cornerRadius = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "background", "background-color" ->
                color(d.value)?.let { s.copy(backgroundColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            "waveform-color", "wave-color", "color" ->
                color(d.value)?.let { s.copy(waveformColor = it) }
                    ?: s.also { ctx.badValue(d, "颜色值") }

            "icon-size" ->
                length(d.value)?.let { s.copy(iconSize = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "waveform-width", "wave-width" ->
                length(d.value)?.let { s.copy(waveformWidth = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "waveform-height", "wave-height" ->
                length(d.value)?.let { s.copy(waveformHeight = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "bar-count", "wave-count" ->
                int(d.value)?.let { s.copy(barCount = it.coerceAtMost(200)) }
                    ?: s.also { ctx.badValue(d, "正整数") }

            "bar-width" ->
                length(d.value)?.let { s.copy(barWidth = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "padding" -> padding(d.value)?.let {
                s.copy(paddingVertical = it.first, paddingHorizontal = it.second)
            } ?: s.also { ctx.badValue(d, "1~4 个长度值") }

            "padding-top", "padding-bottom", "padding-vertical" ->
                length(d.value)?.let { s.copy(paddingVertical = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            "padding-left", "padding-right", "padding-horizontal" ->
                length(d.value)?.let { s.copy(paddingHorizontal = it) }
                    ?: s.also { ctx.badValue(d, "长度值") }

            else -> null
        }
    }

    // endregion

    private class ParseContext {
        val errors = mutableListOf<ChatThemeIssue>()
        val unknown = mutableListOf<ChatThemeIssue>()

        // 一个选择器可能命中多个目标（如 .bubble 同时作用于用户和助手），
        // 同一行的同一条提示只保留一次
        fun error(line: Int, text: String) {
            val issue = ChatThemeIssue(line, text)
            if (errors.size < MAX_ISSUES && issue !in errors) errors += issue
        }

        fun warn(line: Int, text: String) {
            val issue = ChatThemeIssue(line, text)
            if (unknown.size < MAX_ISSUES && issue !in unknown) unknown += issue
        }
    }

    private const val MAX_ISSUES = 100
}
