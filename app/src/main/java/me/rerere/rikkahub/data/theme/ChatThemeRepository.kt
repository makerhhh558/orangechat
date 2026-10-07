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

    /** themes 根目录，取不到外部存储时退回应用私有目录 */
    val themesDir: File
        get() {
            val external = runCatching { context.getExternalFilesDir(null) }.getOrNull()
            val base = external ?: context.filesDir
            return File(base, THEMES_DIR).apply { runCatching { mkdirs() } }
        }

    /** 扫描所有可用主题。任何 IO 异常都退化为空列表。 */
    suspend fun listThemes(): List<ChatThemeEntry> = withContext(Dispatchers.IO) {
        runCatching {
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
        }
    }

    /**
     * 加载指定主题并更新 [loadState]。
     *
     * [themeName] 为空、主题不存在、css 读不出来或解析彻底失败时，一律回退内置默认样式。
     */
    suspend fun reload(themeName: String) = withContext(Dispatchers.IO) {
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
        if (unknown.isNotEmpty()) {
            Log.i(TAG, "主题 $themeName 有 ${unknown.size} 条未生效的样式: ${unknown.joinToString()}")
        }
        if (errors.isNotEmpty()) {
            Log.w(TAG, "主题 $themeName 有 ${errors.size} 处问题: ${errors.joinToString()}")
        }
        return ChatThemeLoadState(
            spec = result.spec,
            themeName = themeName,
            errors = errors,
            unknownProperties = unknown,
        )
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
                if (isZip) importZip(uri, baseName) else importCss(uri, baseName, name)
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
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val entryName = entry.name.replace('\\', '/')
                    if (entry.isDirectory) continue
                    val fileName = entryName.substringAfterLast('/')
                    if (fileName.isBlank() || fileName.startsWith(".")) continue
                    // 只接受 css，顺带防 zip slip
                    if (!fileName.endsWith(".css", ignoreCase = true)) continue
                    val target = File(dir, fileName.sanitizeFileName())
                    if (!target.canonicalPath.startsWith(dir.canonicalPath + File.separator)) continue
                    target.outputStream().use { zip.copyTo(it) }
                    cssCount++
                }
            }
        } ?: error("无法读取所选文件")
        if (cssCount == 0) {
            dir.deleteRecursively()
            error("压缩包里没有 .css 文件")
        }
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
            dir.isDirectory && dir.canonicalPath.startsWith(themesDir.canonicalPath) &&
                dir.deleteRecursively()
        }.getOrElse { false }
    }

    /** 往 themes 目录写一份示例主题，方便用户照着改。 */
    suspend fun createSampleTheme(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = uniqueDir("sample")
            dir.mkdirs()
            File(dir, "style.css").writeText(SAMPLE_CSS)
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

        /** 示例主题：把当前支持的选择器和属性全写一遍，当作活文档 */
        val SAMPLE_CSS = """
/* 橘瓣聊天主题示例
 * 单位可写 12 / 12dp / 12px，颜色支持 #rgb #rrggbb #rrggbbaa rgb() rgba() 和常用色名。
 * 不认识的属性会被静默忽略，并在主题页列出来。
 */

/* 消息列表整体布局 */
.message-list {
    gap: 12dp;            /* 相邻消息的间隔 */
    padding: 16dp;        /* 列表左右内边距 */
}

/* 两侧气泡的公共样式 */
.bubble {
    border-radius: 18dp;
    padding: 8dp 12dp;    /* 纵向 横向 */
    max-width: 85%;
    tail-size: 7dp;       /* 小尖钩大小，0 表示不要尖钩 */
    tail-position: auto;  /* auto 按消息方向自动贴边 */
    tail-shape: triangle; /* triangle | right-triangle */
    tail-offset: 14dp;    /* 尖钩沿边缘的偏移 */
    segment-spacing: 4dp; /* 一条消息拆成多个气泡时的间隔 */
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
