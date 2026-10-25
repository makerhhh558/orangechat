/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.utils

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage

/**
 * 从 AI 回复里摘出第一段完整的 `<cloud>...</cloud>`。
 *
 * 只在渲染时调用，数据库里仍存原文。标签大小写不敏感。
 * 没闭合的标签（含流式输出还没写完的）整段隐藏，残缺的标签开头也不露出来。
 */
data class CloudThoughtResult(
    /** 第一对完整标签的内容，去掉首尾空白；没有或为空时是 null */
    val cloud: String?,
    /** 去掉全部 cloud 标签后的正文 */
    val body: String,
)

private val COMPLETE_TAG = Regex(
    "<cloud\\b[^>]*>(.*?)</cloud\\s*>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val OPEN_TAG = Regex("<cloud\\b[^>]*>", RegexOption.IGNORE_CASE)
private val DANGLING_OPEN = Regex("<cloud\\b[^>]*$", RegexOption.IGNORE_CASE)
private val STRAY_CLOSE = Regex("</cloud\\s*>", RegexOption.IGNORE_CASE)

/** 从长到短，只认 `<cloud` / `</cloud` 的前缀，避免把句尾的 `<d` 之类误删。 */
private val PARTIAL_TAILS = listOf(
    "</cloud", "</clou", "</clo", "</cl", "</c", "</",
    "<cloud", "<clou", "<clo", "<cl", "<c", "<",
)

fun parseCloudThought(text: String): CloudThoughtResult {
    val first = COMPLETE_TAG.find(text)
    val cloud = first?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    // 先摘掉所有完整标签，cloud 里的换行不会流进正文
    val stripped = STRAY_CLOSE.replace(COMPLETE_TAG.replace(text, ""), "")
    val body = stripDanglingCloud(stripped).trimStart()
    return CloudThoughtResult(cloud = cloud, body = body)
}

/**
 * 去掉没闭合的 `<cloud>` 及其后的全部内容，以及末尾残缺的标签开头（如 `<clo`、`</clo`）。
 */
private fun stripDanglingCloud(text: String): String {
    val open = OPEN_TAG.find(text)
    val cut = if (open != null) text.substring(0, open.range.first) else text
    val dangling = DANGLING_OPEN.find(cut)
    val withoutOpen = if (dangling != null) cut.substring(0, dangling.range.first) else cut
    val lower = withoutOpen.lowercase()
    val tail = PARTIAL_TAILS.firstOrNull { lower.endsWith(it) } ?: return withoutOpen
    return withoutOpen.dropLast(tail.length)
}

/** 去掉心里话标签后的正文。复制、朗读、通知、导出都走这里，避免把标签念出来。 */
fun String.stripCloud(): String = parseCloudThought(this).body

/**
 * 展示用文本。助手消息摘掉心里话，其余角色原样返回。
 * 发给模型的历史仍用 [UIMessage.toText]，这里只给界面和朗读。
 */
fun UIMessage.toDisplayText(): String =
    if (role == MessageRole.ASSISTANT) toText().stripCloud() else toText()

/**
 * ☁️ 内容里的一块。只在 `<cloud>` 内部解析，标签之外的文字仍走 markdown。
 */
sealed interface CloudBlock {
    /** 一段普通文字，交给 markdown 渲染 */
    data class Markdown(val text: String) : CloudBlock

    /** `<mood>` 心情胶囊。空内容在解析时丢掉，不会出现在结果里 */
    data class Mood(val text: String) : CloudBlock

    /** `<bar>` 进度条。value 已夹到 0~100 */
    data class Bar(val label: String, val value: Float) : CloudBlock
}

/** 进度条填充的分档。40 及以下 low，75 及以下 mid，其余 high。 */
enum class CloudBarTier { LOW, MID, HIGH }

fun cloudBarTier(value: Float): CloudBarTier = when {
    value <= 40f -> CloudBarTier.LOW
    value <= 75f -> CloudBarTier.MID
    else -> CloudBarTier.HIGH
}

private val MOOD_TAG = Regex(
    "<mood\\b[^>]*>(.*?)</mood\\s*>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val BAR_TAG = Regex(
    "<bar\\b([^<>]*?)/?>(?:\\s*</bar\\s*>)?",
    RegexOption.IGNORE_CASE,
)
private val BAR_ATTR = Regex("(\\w+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')")
private val BROKEN_MOOD_OPEN = Regex("<mood\\b[^>]*>", RegexOption.IGNORE_CASE)
private val STRAY_MOOD_CLOSE = Regex("</mood\\s*>", RegexOption.IGNORE_CASE)
private val BROKEN_BAR = Regex("<bar\\b[^>\\n]*", RegexOption.IGNORE_CASE)
private val STRAY_BAR_CLOSE = Regex("</bar\\s*>", RegexOption.IGNORE_CASE)

/**
 * 把 ☁️ 内容拆成文字、心情胶囊和进度条，保持原文顺序。
 *
 * 没有自定义标签时返回一整块 [CloudBlock.Markdown]，内容和传入的字符串一致。
 * 缺 label、value 不是数字、标签写残的进度条直接丢掉，原始标签文字不外露。
 */
fun parseCloudContent(cloud: String): List<CloudBlock> {
    data class Hit(val range: IntRange, val block: CloudBlock?)

    val hits = mutableListOf<Hit>()
    MOOD_TAG.findAll(cloud).forEach { match ->
        val text = match.groupValues[1].trim()
        hits += Hit(match.range, text.takeIf { it.isNotEmpty() }?.let { CloudBlock.Mood(it) })
    }
    BAR_TAG.findAll(cloud).forEach { match ->
        if (hits.any { it.range.first <= match.range.last && match.range.first <= it.range.last }) return@forEach
        // 成对写法把到 </bar> 为止的内容一起吃掉，中间的文字不露出来
        var end = match.range.last + 1
        if (!match.value.trimEnd().endsWith("/>") && !match.value.contains("</bar", ignoreCase = true)) {
            val close = cloud.indexOf("</bar>", end, ignoreCase = true)
            if (close >= 0) end = close + "</bar>".length
        }
        hits += Hit(match.range.first until end, parseBar(match.groupValues[1]))
    }
    if (hits.isEmpty()) {
        // 没有标签时原样返回，渲染结果和改动前一致
        if ('<' !in cloud) return listOf(CloudBlock.Markdown(cloud))
        val cleaned = cleanStrayTags(cloud)
        return listOf(CloudBlock.Markdown(cleaned.ifEmpty { cloud }))
    }

    val blocks = mutableListOf<CloudBlock>()
    var cursor = 0
    hits.sortedBy { it.range.first }.forEach { hit ->
        if (hit.range.first < cursor) return@forEach
        appendText(blocks, cloud.substring(cursor, hit.range.first))
        hit.block?.let { blocks += it }
        cursor = hit.range.last + 1
    }
    appendText(blocks, cloud.substring(cursor))
    return blocks.ifEmpty { listOf(CloudBlock.Markdown("")) }
}

/** 读 label 和 value。缺 label 或 value 不是数字时返回 null，调用方丢掉整条。 */
private fun parseBar(attrs: String): CloudBlock.Bar? {
    var label: String? = null
    var value: Float? = null
    BAR_ATTR.findAll(attrs).forEach { match ->
        val raw = match.groupValues[2].ifEmpty { match.groupValues[3] }
        when (match.groupValues[1].lowercase()) {
            "label" -> label = raw.trim()
            "value" -> value = raw.trim().toFloatOrNull()
        }
    }
    val name = label?.takeIf { it.isNotEmpty() } ?: return null
    val number = value ?: return null
    return CloudBlock.Bar(name, number.coerceIn(0f, 100f))
}

/** 标签之间的文字：清掉写残的标签，去掉两端的换行，空白段不单独成块。 */
private fun appendText(blocks: MutableList<CloudBlock>, raw: String) {
    val text = cleanStrayTags(raw).trim('\n', '\r')
    if (text.isNotBlank()) blocks += CloudBlock.Markdown(text)
}

private fun cleanStrayTags(text: String): String =
    STRAY_BAR_CLOSE.replace(
        BROKEN_BAR.replace(
            STRAY_MOOD_CLOSE.replace(BROKEN_MOOD_OPEN.replace(text, ""), ""),
            "",
        ),
        "",
    )
