/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/** themes 目录下的一套主题（一个子文件夹）。 */
data class ChatThemeEntry(
    val name: String,
    val directory: File,
    /** 该主题下所有 .css 文件，按文件名排序 */
    val cssFiles: List<File>,
)

/** 当前生效主题的加载状态，供设置页展示。 */
data class ChatThemeLoadState(
    val spec: ChatThemeSpec = ChatThemeSpec.BUILTIN,
    /** 空字符串 = 使用内置默认样式 */
    val themeName: String = "",
    val errors: List<ChatThemeIssue> = emptyList(),
    val unknownProperties: List<ChatThemeIssue> = emptyList(),
    /** 主题目录存在但没找到可用的 css 时为 true */
    val missing: Boolean = false,
)

/**
 * 聊天主题仓库。
 *
 * 主题放在外部目录 `/sdcard/Android/data/<包名>/files/themes/<主题名>/` 下的 css 文件，
 * 用户可以直接用文件管理器编辑；安卓 11 之后部分文件管理器进不去该目录，
 * 所以同时提供 [importTheme] 从任意位置导入 css 或 zip。
 *
 * [loadState] 是界面渲染的唯一数据源，调用 [reload] 即可热更新，无需重启应用。
 */
class ChatThemeRepository(private val context: Context) {

    private val _loadState = MutableStateFlow(ChatThemeLoadState())
    val loadState: StateFlow<ChatThemeLoadState> = _loadState.asStateFlow()

    /**
     * 当前 themes 目录下的主题列表。
     *
     * 由仓库自己持有而不是让设置页用 `rememberCoroutineScope` 去拉：
     * 导入要走系统文件选择器，Activity 切到后台可能把页面的作用域取消，
     * 于是文件已经写进磁盘、列表却没刷新（表现为「样式生效但列表显示还没有主题」）。
     * 放在这里，任何一次增删改都会在同一个 IO 块内同步刷新。
     */
    private val _themes = MutableStateFlow<List<ChatThemeEntry>>(emptyList())
    val themes: StateFlow<List<ChatThemeEntry>> = _themes.asStateFlow()

    /** themes 根目录，取不到外部存储时退回应用私有目录 */
    val themesDir: File
        get() {
            val external = runCatching { context.getExternalFilesDir(null) }.getOrNull()
            val base = external ?: context.filesDir
            return File(base, THEMES_DIR).apply { runCatching { mkdirs() } }
        }

    /** 扫描所有可用主题。任何 IO 异常都退化为空列表。 */
    suspend fun listThemes(): List<ChatThemeEntry> = withContext(Dispatchers.IO) {
        scanThemes().also { _themes.value = it }
    }

    /** 重新扫描目录并更新 [themes]。 */
    suspend fun refreshThemes() {
        listThemes()
    }

    /**
     * 同步扫描 themes 目录。
     *
     * 刻意不是 suspend：增删导入之后要能在同一个 IO 块里立刻刷新，
     * 中间不留挂起点，避免作用域被取消时漏掉刷新。
     */
    private fun scanThemes(): List<ChatThemeEntry> = runCatching {
        themesDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.mapNotNull { dir ->
                val css = dir.listFiles()
                    ?.filter { it.isFile && it.extension.equals("css", ignoreCase = true) }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
                ChatThemeEntry(name = dir.name, directory = dir, cssFiles = css)
            }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()
    }.getOrElse {
        Log.w(TAG, "扫描主题目录失败", it)
        emptyList()
    }.also {
        Log.d(TAG, "扫描主题目录 ${themesDir.absolutePath}，得到 ${it.size} 个: ${it.joinToString { e -> e.name }}")
    }

    /**
     * 加载指定主题并更新 [loadState]。
     *
     * [themeName] 为空、主题不存在、css 读不出来或解析彻底失败时，一律回退内置默认样式。
     */
    suspend fun reload(themeName: String) = withContext(Dispatchers.IO) {
        // 重新加载顺带重扫目录：用户可能直接用文件管理器增删了主题文件夹
        _themes.value = scanThemes()
        if (themeName.isBlank()) {
            _loadState.value = ChatThemeLoadState()
            return@withContext
        }
        val state = runCatching { loadInternal(themeName) }.getOrElse { e ->
            Log.e(TAG, "加载主题 $themeName 失败", e)
            ChatThemeLoadState(
                themeName = themeName,
                errors = listOf(ChatThemeIssue(0, "主题读取失败: ${e.message ?: "未知错误"}，已回退内置样式")),
            )
        }
        _loadState.value = state
    }

    private fun loadInternal(themeName: String): ChatThemeLoadState {
        val dir = File(themesDir, themeName)
        if (!dir.isDirectory) {
            return ChatThemeLoadState(
                themeName = themeName,
                missing = true,
                errors = listOf(ChatThemeIssue(0, "找不到主题文件夹 \"$themeName\"，已回退内置样式")),
            )
        }
        val cssFiles = dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("css", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        if (cssFiles.isEmpty()) {
            return ChatThemeLoadState(
                themeName = themeName,
                missing = true,
                errors = listOf(ChatThemeIssue(0, "主题 \"$themeName\" 里没有 .css 文件，已回退内置样式")),
            )
        }

        val errors = mutableListOf<ChatThemeIssue>()
        val unknown = mutableListOf<ChatThemeIssue>()
        // 多个 css 按文件名顺序拼接，后面的声明覆盖前面的，和 CSS 直觉一致
        val sb = StringBuilder()
        for (file in cssFiles) {
            val text = runCatching { file.readText() }.getOrElse { e ->
                errors += ChatThemeIssue(0, "${file.name} 读取失败: ${e.message ?: "文件可能已损坏"}")
                null
            } ?: continue
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(text)
        }
        if (sb.isBlank()) {
            return ChatThemeLoadState(
                themeName = themeName,
                missing = true,
                errors = errors + ChatThemeIssue(0, "主题 \"$themeName\" 内容为空，已回退内置样式"),
            )
        }

        val result = ChatThemeCssParser.parse(sb.toString(), themeName)
        errors += result.errors
        unknown += result.unknownProperties
        // baseDir 供 css 里的相对路径（字体、图片）定位
        val spec = result.spec.copy(baseDir = dir.absolutePath)
        errors += checkFontFiles(spec, dir)
        errors += checkStickerImages(spec, dir)
        errors += checkAvatarFrameImages(spec, dir)
        errors += checkBorderImages(spec, dir)
        if (unknown.isNotEmpty()) {
            Log.i(TAG, "主题 $themeName 有 ${unknown.size} 条未生效的样式: ${unknown.joinToString()}")
        }
        if (errors.isNotEmpty()) {
            Log.w(TAG, "主题 $themeName 有 ${errors.size} 处问题: ${errors.joinToString()}")
        }
        return ChatThemeLoadState(
            spec = spec,
            themeName = themeName,
            errors = errors,
            unknownProperties = unknown,
        )
    }

    /**
     * 校验 css 里引用的字体文件是否存在。
     *
     * 加载阶段就报出来，用户在主题设置页能直接看到「字体找不到」，
     * 而不是等到渲染时静默回退默认字体却不知道为什么。
     * 实际的字体构建在渲染侧（见 `rememberCssFontFamily`），这里只做存在性检查。
     */
    private fun checkFontFiles(spec: ChatThemeSpec, dir: File): List<ChatThemeIssue> {
        val issues = mutableListOf<ChatThemeIssue>()
        listOf("用户气泡" to spec.userBubble, "助手气泡" to spec.assistantBubble).forEach { (label, bubble) ->
            val font = bubble.fontFamily
            if (font is CssFontFamily.File) {
                val ok = runCatching {
                    resolveThemeFile(dir, font.path)?.isFile == true
                }.getOrDefault(false)
                if (!ok) {
                    issues += ChatThemeIssue(0, "$label 的字体文件 \"${font.path}\" 找不到或无法读取，已回退默认字体")
                }
            }
        }
        return issues
    }

    /**
     * 校验 css 里引用的贴纸图片。
     *
     * 只管得了本地路径：加载阶段就能判断文件在不在，和 [checkFontFiles] 一个思路。
     * 远程地址（http/https）这里一律放过 —— 能不能拉到只有真正发请求时才知道，
     * 在加载阶段探测会把「当时没网」误报成「图有问题」，所以交给渲染侧
     * [reportStickerLoadFailure] 在 coil 真的失败之后再报。
     *
     * 没写 sticker 或写了 `none`（source 为 null）属于「未配置」，不报错也不画。
     */
    private fun checkStickerImages(spec: ChatThemeSpec, dir: File): List<ChatThemeIssue> {
        val issues = mutableListOf<ChatThemeIssue>()
        listOf("用户气泡" to spec.userBubble, "助手气泡" to spec.assistantBubble).forEach { (label, bubble) ->
            val source = bubble.sticker.source?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            if (source.startsWith("http://", ignoreCase = true) ||
                source.startsWith("https://", ignoreCase = true)
            ) {
                return@forEach
            }
            val ok = runCatching { resolveThemeFile(dir, source)?.isFile == true }.getOrDefault(false)
            if (!ok) {
                issues += ChatThemeIssue(
                    bubble.sticker.sourceLine,
                    "$label 的贴纸图片 \"$source\" 找不到或无法读取，该贴纸不会显示",
                )
            }
        }
        return issues
    }

    /**
     * 校验 css 里引用的头像挂件图片。规则与 [checkStickerImages] 相同：
     * 只查本地相对路径，远程地址交给渲染侧 [reportAvatarFrameLoadFailure]。
     * 没写挂件或写了 `none`（source 为 null）属于未配置，不报错。
     */
    private fun checkAvatarFrameImages(spec: ChatThemeSpec, dir: File): List<ChatThemeIssue> {
        val issues = mutableListOf<ChatThemeIssue>()
        listOf("用户头像" to spec.userAvatar, "助手头像" to spec.assistantAvatar).forEach { (label, avatar) ->
            val source = avatar.frameSource?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            if (source.startsWith("http://", ignoreCase = true) ||
                source.startsWith("https://", ignoreCase = true)
            ) {
                return@forEach
            }
            val ok = runCatching { resolveThemeFile(dir, source)?.isFile == true }.getOrDefault(false)
            if (!ok) {
                issues += ChatThemeIssue(
                    avatar.frameSourceLine,
                    "$label 的挂件图片 \"$source\" 找不到或无法读取，该挂件不会显示",
                )
            }
        }
        return issues
    }

    /**
     * 校验气泡和语音的九宫格皮肤。规则与 [checkStickerImages] 相同：
     * 只查本地相对路径，远程地址交给渲染侧 [reportBorderImageLoadFailure]。
     * 没写或写了 `none` 属于未配置，不报错。
     */
    private fun checkBorderImages(spec: ChatThemeSpec, dir: File): List<ChatThemeIssue> {
        val issues = mutableListOf<ChatThemeIssue>()
        listOf(
            "用户气泡" to spec.userBubble.borderImage,
            "助手气泡" to spec.assistantBubble.borderImage,
            "用户语音" to spec.userVoice.borderImage,
            "助手语音" to spec.assistantVoice.borderImage,
        ).forEach { (label, image) ->
            if (!image.active) return@forEach
            val source = image.source?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            if (source.startsWith("http://", ignoreCase = true) ||
                source.startsWith("https://", ignoreCase = true)
            ) {
                return@forEach
            }
            val ok = runCatching { resolveThemeFile(dir, source)?.isFile == true }.getOrDefault(false)
            if (!ok) {
                issues += ChatThemeIssue(
                    image.sourceLine,
                    "$label 的皮肤图片 \"$source\" 找不到或无法读取，已回退普通气泡",
                )
            }
        }
        return issues
    }

    /**
     * 渲染侧报告远程贴纸加载失败。
     *
     * 远程地址在加载阶段无法判断可用性，所以由 `AsyncImage` 的 onError 回调来报：
     * 报的是 coil 真实的加载结果，不会把断网误判成图片有问题。
     *
     * 同一个地址只记一次（[ChatThemeIssue] 判重），避免滚动时反复追加。
     * 只往当前生效主题的状态里追加，主题已经切走时直接丢弃。
     */
    fun reportStickerLoadFailure(themeName: String, source: String, line: Int) {
        val current = _loadState.value
        if (current.themeName != themeName) return
        val remote = source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true)
        val reason = if (remote) "请检查网络是否能访问该地址" else "文件可能已损坏或不是支持的图片格式"
        val issue = ChatThemeIssue(line, "贴纸图片 \"$source\" 加载失败，$reason")
        if (issue in current.errors) return
        if (current.errors.size >= MAX_RUNTIME_ISSUES) return
        _loadState.value = current.copy(errors = current.errors + issue)
    }

    /**
     * 渲染侧报告头像挂件加载失败。语义与 [reportStickerLoadFailure] 相同，
     * 只是文案区分成「头像挂件图片」，方便在主题页看出来是哪一层没出来。
     */
    fun reportAvatarFrameLoadFailure(themeName: String, source: String, line: Int) {
        val current = _loadState.value
        if (current.themeName != themeName) return
        val remote = source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true)
        val reason = if (remote) "请检查网络是否能访问该地址" else "文件可能已损坏或不是支持的图片格式"
        val issue = ChatThemeIssue(line, "头像挂件图片 \"$source\" 加载失败，$reason")
        if (issue in current.errors) return
        if (current.errors.size >= MAX_RUNTIME_ISSUES) return
        _loadState.value = current.copy(errors = current.errors + issue)
    }

    /**
     * 渲染侧报告九宫格皮肤加载失败。语义与 [reportStickerLoadFailure] 相同，
     * 皮肤不显示，气泡回退到纯色背景。
     */
    fun reportBorderImageLoadFailure(themeName: String, source: String, line: Int) {
        val current = _loadState.value
        if (current.themeName != themeName) return
        val remote = source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true)
        val reason = if (remote) "请检查网络是否能访问该地址" else "文件可能已损坏或不是支持的图片格式"
        val issue = ChatThemeIssue(line, "皮肤图片 \"$source\" 加载失败，$reason")
        if (issue in current.errors) return
        if (current.errors.size >= MAX_RUNTIME_ISSUES) return
        _loadState.value = current.copy(errors = current.errors + issue)
    }

    /**
     * 从任意 Uri 导入主题，支持单个 .css 和打包的 .zip。
     * 返回导入后的主题名，失败时返回带原因的 [Result]。
     */
    suspend fun importTheme(uri: Uri, displayName: String?): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = displayName?.substringAfterLast('/')?.trim().orEmpty()
                val isZip = name.endsWith(".zip", ignoreCase = true)
                val baseName = name.substringBeforeLast('.', name)
                    .ifBlank { "theme_${System.currentTimeMillis()}" }
                    .sanitizeFileName()
                val imported = if (isZip) importZip(uri, baseName) else importCss(uri, baseName, name)
                // 在同一个 IO 块内刷新：调用方的作用域可能在返回前就被取消
                _themes.value = scanThemes()
                imported
            }
        }

    private fun importCss(uri: Uri, baseName: String, rawName: String): String {
        if (rawName.isNotBlank() && !rawName.endsWith(".css", ignoreCase = true)) {
            error("只支持导入 .css 或 .zip 文件")
        }
        val dir = uniqueDir(baseName)
        dir.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            File(dir, "style.css").outputStream().use { input.copyTo(it) }
        } ?: error("无法读取所选文件")
        return dir.name
    }

    private fun importZip(uri: Uri, baseName: String): String {
        val dir = uniqueDir(baseName)
        dir.mkdirs()
        var cssCount = 0
        var assetCount = 0
        val skipped = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val entryName = entry.name.replace('\\', '/')
                    if (entry.isDirectory) continue
                    val rawSegments = entryName.split('/')
                        .filter { it.isNotBlank() && it != "." && it != ".." }
                    if (rawSegments.isEmpty()) continue
                    // 隐藏文件和 __MACOSX 里的 ._xxx 残渣直接丢掉
                    if (rawSegments.any { it.startsWith(".") }) continue
                    val fileName = rawSegments.last()
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    if (ext !in ALLOWED_ZIP_EXTENSIONS) {
                        skipped += fileName
                        continue
                    }
                    // 保留子目录结构：css 里 url("images/cat.png") 这类相对路径要能找到文件，
                    // 扁平化会让路径对不上
                    val target = File(dir, rawSegments.joinToString(File.separator) { it.sanitizeFileName() })
                    // 防 zip slip
                    if (!target.canonicalPath.startsWith(dir.canonicalPath + File.separator)) continue
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                    if (ext == "css") cssCount++ else assetCount++
                }
            }
        } ?: error("无法读取所选文件")
        if (cssCount == 0) {
            dir.deleteRecursively()
            error("压缩包里没有 .css 文件")
        }
        Log.d(
            TAG,
            "导入 zip ${dir.name}: $cssCount 个 css, $assetCount 个资源文件" +
                if (skipped.isEmpty()) "" else "，跳过 ${skipped.size} 个不支持的文件: ${skipped.take(5).joinToString()}"
        )
        return dir.name
    }

    private fun uniqueDir(baseName: String): File {
        val root = themesDir
        var candidate = File(root, baseName)
        var index = 2
        while (candidate.exists()) {
            candidate = File(root, "$baseName-$index")
            index++
        }
        return candidate
    }

    /** 删除一套主题。 */
    suspend fun deleteTheme(themeName: String): Boolean = withContext(Dispatchers.IO) {
        if (themeName.isBlank()) return@withContext false
        runCatching {
            val dir = File(themesDir, themeName)
            val deleted = dir.isDirectory &&
                dir.canonicalPath.startsWith(themesDir.canonicalPath) &&
                dir.deleteRecursively()
            if (deleted) _themes.value = scanThemes()
            deleted
        }.getOrElse { false }
    }

    /** 往 themes 目录写一份示例主题，方便用户照着改。 */
    suspend fun createSampleTheme(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = uniqueDir("sample")
            dir.mkdirs()
            File(dir, "style.css").writeText(SAMPLE_CSS)
            _themes.value = scanThemes()
            dir.name
        }
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[^A-Za-z0-9\\u4e00-\\u9fa5._\\-]"), "_").trim('.', '_', '-')
            .ifBlank { "theme" }
            .take(64)

    companion object {
        private const val TAG = "ChatThemeRepository"
        const val THEMES_DIR = "themes"

        /**
         * 渲染侧追加的运行时问题条数上限。
         *
         * 远程贴纸失败是在滚动过程中陆续报上来的，加个上限避免错误列表无限增长。
         */
        private const val MAX_RUNTIME_ISSUES = 20

        /**
         * zip 导入允许落盘的扩展名。
         *
         * 除了 css 本身，还要放行贴纸/头像挂件用的图片和 font-family 的字体文件，
         * 否则这些资源会被丢掉、css 里的 url() 永远找不到文件。
         * 保持白名单而不是放开全部：避免把可执行文件、apk、脚本之类的东西解到外部目录。
         */
        private val ALLOWED_ZIP_EXTENSIONS = setOf(
            "css",
            // 图片
            "png", "jpg", "jpeg", "webp", "gif", "bmp",
            // 字体
            "ttf", "otf", "ttc",
        )

        /** 示例主题：把当前支持的选择器和属性全写一遍，当作活文档 */
        val SAMPLE_CSS = """
/* 橘瓣聊天主题示例
 * 单位可写 12 / 12dp / 12px，字号还可写 16sp；角度写 10deg。
 * 颜色支持 #rgb #rrggbb #rrggbbaa rgb() rgba() 和常用色名。
 * 图片和字体用 url("xxx")，相对路径从本 css 所在的主题文件夹里找。
 * 不认识的属性会被静默忽略，并在主题页列出来。
 */

/* 消息列表整体布局 */
.message-list {
    gap: 12dp;            /* 相邻消息的间隔，别名 spacing / margin-bottom */
    padding: 16dp;        /* 列表左右内边距 */
}

/* 两侧气泡的公共样式 */
.bubble {
    border-radius: 18dp;  /* 也支持百分比，50% 即为圆形/胶囊 */
    padding: 8dp 12dp;    /* 纵向 横向 */
    max-width: 85%;       /* 气泡最大宽度，不写则不限制 */
    tail-width: 7dp;      /* 尖钩伸出的距离，别名 tail-size，0 表示不要尖钩 */
    tail-height: 14dp;    /* 尖钩底边长度，不写则按 tail-width 推算 */
    tail-position: auto;  /* auto 按消息方向自动贴边 */
    tail-shape: triangle; /* triangle | right-triangle */
    tail-offset: 14dp;    /* 尖钩沿边缘的偏移 */
    segment-spacing: 4dp; /* 一条消息拆成多个气泡时的间隔 */

    /* 阴影：offset-x offset-y [blur] [spread] 颜色，写 none 关闭。
     * 沿气泡实际形状（含尖钩）绘制；Android 9 以下不显示阴影。 */
    box-shadow: 0 2dp 6dp rgba(0, 0, 0, 0.12);

    /* 正文字体，只影响气泡正文，不影响昵称和时间 */
    font-size: 16sp;         /* 标题和代码块会按同样倍数跟着缩放 */
    line-height: 1.4;        /* 倍数或长度 */
    letter-spacing: 0;       /* 长度，可为负 */
}

/* 我方气泡 */
.bubble.user {
    background: #95ec69;
    color: #111111;
    border-radius: 18dp 4dp 18dp 18dp;
}

/* 对方气泡 */
.bubble.assistant {
    background: #ffffff;
    color: #222222;
    border: 1dp solid #e5e5e5;
    border-radius: 4dp 18dp 18dp 18dp;

    font-weight: normal;        /* normal | bold | 100~900 */
    font-family: sans-serif;    /* serif | sans-serif | monospace，或 url("MyFont.ttf") */
    link-color: #4c8ef7;        /* 正文里链接的颜色 */
    code-background: #f2f2f2;   /* 行内代码和代码块的底色，别名 code-bg */
}

/* 贴纸：贴在气泡角上的小图，可以露在气泡外面。
 *
 * sticker          图片，url("cat.png") 或 https://...，none 关闭。
 *                  相对路径从本 css 所在的文件夹里找，图片找不到就不显示贴纸，气泡照常。
 * sticker-size     边长，默认 24dp
 * sticker-position 四个角之一，别名规则同 tail-position
 * sticker-offset   x y，正值往气泡里缩，负值往气泡外伸
 * sticker-rotate   旋转角度，如 10deg / -10deg
 * sticker-rotate-random
 *                  在 sticker-rotate 基础上每条消息随机加减的范围，
 *                  种子取消息 id，同一条消息角度固定、滚动不跳
 * sticker-opacity  0~1
 *
 * 把一张 cat.png 放到本 css 旁边即可看到效果：
 */
.bubble.assistant {
    sticker: url("cat.png");
    sticker-size: 32dp;
    sticker-position: top-right;
    sticker-offset: -10dp -10dp;
    sticker-rotate: 10deg;
    sticker-rotate-random: 8deg;
    sticker-opacity: 0.95;
}

/* 头像 */
.avatar {
    shape: rounded;       /* circle | rounded | square */
    border-radius: 10dp;
    gap: 8dp;
    position: side;       /* top = 头像在气泡上方, side = 头像与气泡并排 */
}

.avatar.user {
    size: 40dp;
}

.avatar.assistant {
    size: 40dp;
    border: 1dp solid #e0e0e0;
}

/* 头像挂件：叠在头像上层、居中，比头像大的部分不裁切，也不改变头像占位。
 *
 * avatar-frame        图片，url("frame.png") 或 https://...，none 关闭
 *                     （none 连「显示设置」里的挂件也一起关掉）。
 *                     相对路径从本 css 所在的文件夹里找，找不到就不显示挂件。
 * avatar-frame-size   长度或百分比，默认 140%
 * avatar-frame-offset 两个长度 x y，可负，相对头像中心
 */

/* 语音消息气泡 */
.voice {
    border-radius: 16dp;
    padding: 8dp 12dp;
    icon-size: 24dp;
    waveform-width: 60dp;
    waveform-height: 24dp;
    waveform-color: #4c8ef7;
    bar-count: 24;
    bar-width: 2.5dp;
}
""".trimIndent()
    }
}
