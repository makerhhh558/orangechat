/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.File

/**
 * 解析主题 css 里的相对路径。
 *
 * 只允许指向主题文件夹内部：用 canonicalPath 做包含性检查，
 * 挡掉 `../../databases/xxx` 这类越界引用（和 zip 导入的防护思路一致）。
 *
 * @param baseDir 主题文件夹
 * @param path css 里写的路径，可带 `./` 前缀
 * @return 落在主题文件夹内且存在的文件，否则 null
 */
fun resolveThemeFile(baseDir: File?, path: String): File? {
    if (baseDir == null) return null
    val raw = path.trim().trim('"', '\'').trim()
    if (raw.isEmpty()) return null
    // 绝对路径与 content/http 之类的 scheme 一律不当本地相对路径处理
    if (raw.startsWith("/") || raw.contains("://")) return null
    return runCatching {
        val target = File(baseDir, raw)
        val root = baseDir.canonicalPath
        val canonical = target.canonicalPath
        if (canonical != root && !canonical.startsWith(root + File.separator)) return null
        target.takeIf { it.isFile }
    }.getOrNull()
}

/** 同上，baseDir 以路径字符串给出（[ChatThemeSpec.baseDir]）。 */
fun resolveThemeFile(baseDir: String, path: String): File? =
    resolveThemeFile(baseDir.takeIf { it.isNotBlank() }?.let { File(it) }, path)

/**
 * 把 css 里的图片引用（贴纸、头像挂件）变成 coil 能吃的 model。
 *
 * 支持两种写法：
 * - `https://...`：原样交给 coil 去下载
 * - 相对路径：从主题文件夹里找，返回 [File]
 *
 * 返回 null 表示没写、或本地文件不存在 —— 调用方据此直接不画这个图层，
 * 保证图片缺失时界面照常显示。
 */
fun resolveThemeImageModel(source: String?, baseDir: String): Any? {
    val raw = source?.trim()?.trim('"', '\'')?.trim().orEmpty()
    if (raw.isEmpty() || raw.equals("none", ignoreCase = true)) return null
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
        return raw
    }
    return resolveThemeFile(baseDir, raw)
}

/** [resolveThemeImageModel] 的 Compose 包装，避免每次重组都碰文件系统。 */
@Composable
fun rememberThemeImageModel(source: String?, baseDir: String): Any? =
    remember(source, baseDir) { resolveThemeImageModel(source, baseDir) }

/**
 * 把九宫格皮肤解码成位图，供 [nineSlice] 按原图像素切片。
 *
 * 没写、写了 `none`、或本地文件不存在时返回 null，调用方回退普通气泡。
 * 本地缺失在主题加载阶段已经报过，这里不再重复；网络失败或文件损坏由 [onFailure] 报一次。
 */
@Composable
fun rememberBorderImageBitmap(
    borderImage: BorderImage,
    baseDir: String,
    onFailure: () -> Unit,
): ImageBitmap? {
    val source = borderImage.source?.takeIf { borderImage.active }
    val model = rememberThemeImageModel(source, baseDir)
    val context = LocalContext.current
    val failure = rememberUpdatedState(onFailure)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, model) {
        val current = model
        if (current == null) {
            value = null
            return@produceState
        }
        value = loadBorderImage(context, current)
        if (value == null) failure.value.invoke()
    }
    return bitmap
}

private suspend fun loadBorderImage(context: Context, model: Any): ImageBitmap? = runCatching {
    val request = ImageRequest.Builder(context)
        .data(model)
        .allowHardware(false)
        .build()
    val result = SingletonImageLoader.get(context).execute(request)
    (result as? SuccessResult)?.image?.toBitmap()?.asImageBitmap()
}.getOrNull()

/**
 * 把 css 的 `font-family` 变成 Compose 的 [FontFamily]。
 *
 * 返回 null 表示用应用默认字体 —— 没写 font-family、写了通用族 default、
 * 字体文件找不到或加载失败都走这条路，保证任何情况下不崩。
 * 文件缺失会在主题加载阶段由 [ChatThemeRepository] 报一条错误，这里不重复提示。
 */
@Composable
fun rememberCssFontFamily(font: CssFontFamily?, baseDir: String): FontFamily? {
    return remember(font, baseDir) {
        when (font) {
            null -> null
            is CssFontFamily.Generic -> when (font.name) {
                "serif" -> FontFamily.Serif
                "sans-serif" -> FontFamily.SansSerif
                "monospace" -> FontFamily.Monospace
                "cursive" -> FontFamily.Cursive
                else -> null
            }

            is CssFontFamily.File -> {
                val file = resolveThemeFile(baseDir, font.path)
                if (file == null) null
                else runCatching { FontFamily(Font(file)) }.getOrNull()
            }
        }
    }
}
