/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import me.rerere.rikkahub.data.theme.ChatThemeSpec
import me.rerere.rikkahub.data.theme.CloudBoxStyle
import me.rerere.rikkahub.data.theme.CloudPopupStyle
import me.rerere.rikkahub.data.theme.CloudTextStyle
import me.rerere.rikkahub.data.theme.CssGradient
import me.rerere.rikkahub.data.theme.CssTextUnit
import me.rerere.rikkahub.utils.CloudBarTier
import me.rerere.rikkahub.utils.CloudBlock
import me.rerere.rikkahub.utils.cloudBarTier
import me.rerere.rikkahub.utils.parseCloudContent
import kotlin.math.roundToInt
import me.rerere.rikkahub.data.theme.bubbleGradient
import me.rerere.rikkahub.data.theme.bubbleShadow
import me.rerere.rikkahub.data.theme.rememberCssFontFamily
import me.rerere.rikkahub.data.theme.rememberThemeImageModel
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.context.LocalChatThemeSpec
import me.rerere.rikkahub.ui.context.LocalDisplaySettings

/** 图标占位宽度。气泡最大宽度要扣掉这块，避免宽气泡把图标挤出屏幕。 */
val CLOUD_ICON_SLOT = 28.dp

/**
 * 当前生效的聊天主题。没开 CSS 主题时退回内置默认，保证图标和弹窗仍有样式。
 */
@Composable
fun currentCloudTheme(): ChatThemeSpec {
    val settings = LocalDisplaySettings.current
    val spec = LocalChatThemeSpec.current
    return if (settings.chatThemeEnabled) spec else ChatThemeSpec.BUILTIN
}

/** ☁️ 图标。配了图片就用图片，加载失败或没配时退回 ☁️ 字符。 */
@Composable
fun CloudThoughtIcon(
    spec: ChatThemeSpec,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val style = spec.cloudIcon
    val model = rememberThemeImageModel(style.imageSource, spec.baseDir)
    var failed by remember(model) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .offset(style.offsetX.dp, style.offsetY.dp)
            .size(style.size.dp)
            .alpha(style.opacity.coerceIn(0f, 1f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null && !failed) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.size(style.size.dp),
            )
        } else {
            Text(text = "☁️", fontSize = (style.size * 0.8f).sp)
        }
    }
}

/**
 * 弹窗里的内容。聊天页把它放进 [Popup]，设置页预览直接平铺，两边共用这一份。
 */
@Composable
fun CloudPopupContent(
    text: String,
    spec: ChatThemeSpec,
    modifier: Modifier = Modifier,
) {
    val style = spec.cloudPopup
    val shape = cloudPopupShape(style)
    val borderColor = style.borderColor?.let { it.toComposeColor() }
    val background = style.backgroundColor?.let { it.toComposeColor() }
        ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val alpha = style.backgroundOpacity.coerceIn(0f, 1f)
    val gradient = style.backgroundGradient
    val textStyle = cloudTextStyle(spec.cloudText, spec.baseDir)

    BubbleStickerBox(sticker = style.sticker, seed = 0) {
        Surface(
            modifier = modifier
                .bubbleShadow(style.shadow, shape)
                .then(
                    if (gradient != null) Modifier.bubbleGradient(gradient, shape, alpha) else Modifier
                )
                .widthIn(max = style.maxWidth.dp),
            shape = shape,
            color = if (gradient != null) Color.Transparent else background.copy(alpha = alpha),
            border = if (style.borderWidth > 0f && borderColor != null) {
                BorderStroke(style.borderWidth.dp, borderColor)
            } else null,
        ) {
            val blocks = remember(text) { parseCloudContent(text) }
            val plain = blocks.singleOrNull() as? CloudBlock.Markdown
            // 没有心情胶囊和进度条时保持原来的整段 markdown，渲染结果不变
            if (plain != null) {
                ProvideTextStyle(textStyle) {
                    MarkdownBlock(
                        content = plain.text,
                        modifier = Modifier.padding(
                            PaddingValues(
                                horizontal = style.paddingHorizontal.dp,
                                vertical = style.paddingVertical.dp,
                            )
                        ),
                        style = textStyle,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(
                        PaddingValues(
                            horizontal = style.paddingHorizontal.dp,
                            vertical = style.paddingVertical.dp,
                        )
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProvideTextStyle(textStyle) {
                        blocks.forEach { block ->
                            when (block) {
                                is CloudBlock.Markdown -> MarkdownBlock(content = block.text, style = textStyle)
                                is CloudBlock.Mood -> CloudMoodChip(block.text, spec)
                                is CloudBlock.Bar -> CloudBarLine(block, spec)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 气泡外侧的 ☁️。占固定宽度，点击弹出内容，再点一次或点外面关闭。
 * 弹窗靠近屏幕边缘时自动往里挪。
 */
@Composable
fun CloudThoughtAnchor(
    cloud: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentCloudTheme()
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier.width(CLOUD_ICON_SLOT), contentAlignment = Alignment.Center) {
        CloudThoughtIcon(spec = spec, onClick = { open = !open })
        if (open) {
            Popup(
                popupPositionProvider = ClampedPopupPositionProvider(margin = 8.dp),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                CloudPopupContent(text = cloud, spec = spec)
            }
        }
    }
}

/**
 * 气泡和 ☁️ 的一行。没内容时只画气泡，不占图标的位置。
 */
@Composable
fun CloudThoughtRow(
    cloud: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (cloud == null) {
        content()
        return
    }
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) { content() }
        CloudThoughtAnchor(cloud)
    }
}

/** 弹窗位置：默认贴在锚点上方，超出窗口时往里收。 */
private class ClampedPopupPositionProvider(private val margin: Dp) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val marginPx = margin.value.toInt() * 3
        var x = anchorBounds.right - popupContentSize.width
        var y = anchorBounds.top - popupContentSize.height - marginPx
        if (y < marginPx) y = anchorBounds.bottom + marginPx
        x = x.coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
        y = y.coerceIn(marginPx, (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx))
        return IntOffset(x, y)
    }
}

/** 心情胶囊。没写主题时是浅色圆角小标签。 */
@Composable
private fun CloudMoodChip(text: String, spec: ChatThemeSpec) {
    val style = spec.cloudMood
    val shape = cloudBoxShape(style, fallbackPercent = 50)
    val (color, gradient) = cloudBoxBackground(style, MaterialTheme.colorScheme.secondaryContainer)
    val alpha = (style.opacity ?: 1f).coerceIn(0f, 1f)
    val textStyle = cloudBoxTextStyle(
        style,
        spec.baseDir,
        12.sp,
        MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Surface(
        modifier = Modifier
            .bubbleShadow(style.shadow, shape)
            .then(if (gradient != null) Modifier.bubbleGradient(gradient, shape, alpha) else Modifier),
        shape = shape,
        color = color,
        border = cloudBoxBorder(style),
    ) {
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier.padding(
                horizontal = (style.paddingHorizontal ?: 8f).dp,
                vertical = (style.paddingVertical ?: 2f).dp,
            ),
        )
    }
}

/** 一条进度条：名字、轨道、数值。连续多条由外层 Column 竖着排。 */
@Composable
private fun CloudBarLine(bar: CloudBlock.Bar, spec: ChatThemeSpec) {
    val row = spec.cloudBarRow
    val shape = cloudBoxShape(row, fallbackRadius = 0.dp)
    val (color, gradient) = cloudBoxBackground(row, Color.Transparent)
    val alpha = (row.opacity ?: 1f).coerceIn(0f, 1f)
    val hasBackground = row.backgroundColor != null || gradient != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bubbleShadow(row.shadow, shape)
            .then(if (gradient != null) Modifier.bubbleGradient(gradient, shape, alpha) else Modifier)
            .then(if (hasBackground && gradient == null) Modifier.background(color, shape) else Modifier)
            .then(cloudBoxBorderModifier(row, shape))
            .padding(
                horizontal = (row.paddingHorizontal ?: 0f).dp,
                vertical = (row.paddingVertical ?: 0f).dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CloudBoxLabel(bar.label, spec.cloudBarLabel, spec.baseDir, MaterialTheme.colorScheme.onSurfaceVariant)
        CloudBarTrack(bar, spec, Modifier.weight(1f))
        CloudBoxLabel(
            "${bar.value.roundToInt()}%",
            spec.cloudBarValue,
            spec.baseDir,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CloudBarTrack(bar: CloudBlock.Bar, spec: ChatThemeSpec, modifier: Modifier) {
    val style = spec.cloudBar
    val trackHeight = (style.height ?: 6f).dp
    val shape = cloudBoxShape(style, fallbackPercent = 50)
    val (color, gradient) = cloudBoxBackground(style, MaterialTheme.colorScheme.surfaceVariant)
    val alpha = (style.opacity ?: 1f).coerceIn(0f, 1f)
    val fill = when (cloudBarTier(bar.value)) {
        CloudBarTier.LOW -> spec.cloudBarFillLow
        CloudBarTier.MID -> spec.cloudBarFillMid
        CloudBarTier.HIGH -> spec.cloudBarFillHigh
    }.mergeOver(spec.cloudBarFill)
    Box(
        modifier = modifier
            .height(trackHeight)
            .bubbleShadow(style.shadow, shape)
            .then(cloudBoxBorderModifier(style, shape))
            .clip(shape)
            .then(
                if (gradient != null) Modifier.bubbleGradient(gradient, shape, alpha)
                else Modifier.background(color)
            ),
    ) {
        val fillShape = cloudBoxShape(fill, fallbackPercent = 50).let { written ->
            if (fill.cornerRadius == null && fill.cornerRadiusPercent == null) shape else written
        }
        val (fillColor, fillGradient) = cloudBoxBackground(fill, MaterialTheme.colorScheme.primary)
        val fillAlpha = (fill.opacity ?: 1f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(bar.value / 100f)
                .clip(fillShape)
                .then(
                    if (fillGradient != null) Modifier.bubbleGradient(fillGradient, fillShape, fillAlpha)
                    else Modifier.background(fillColor)
                ),
        )
    }
}

@Composable
private fun CloudBoxLabel(text: String, style: CloudBoxStyle, baseDir: String, fallbackColor: Color) {
    val textStyle = cloudBoxTextStyle(style, baseDir, 12.sp, fallbackColor)
    val shape = cloudBoxShape(style, fallbackRadius = 0.dp)
    val (color, gradient) = cloudBoxBackground(style, Color.Transparent)
    val alpha = (style.opacity ?: 1f).coerceIn(0f, 1f)
    val hasBackground = style.backgroundColor != null || gradient != null
    Text(
        text = text,
        style = textStyle,
        modifier = Modifier
            .bubbleShadow(style.shadow, shape)
            .then(if (gradient != null) Modifier.bubbleGradient(gradient, shape, alpha) else Modifier)
            .then(if (hasBackground && gradient == null) Modifier.background(color, shape) else Modifier)
            .then(cloudBoxBorderModifier(style, shape))
            .padding(
                horizontal = (style.paddingHorizontal ?: 0f).dp,
                vertical = (style.paddingVertical ?: 0f).dp,
            ),
    )
}

@Composable
private fun cloudBoxShape(
    style: CloudBoxStyle,
    fallbackRadius: Dp = 0.dp,
    fallbackPercent: Int? = null,
): Shape {
    val percent = style.cornerRadiusPercent
    return when {
        percent != null -> RoundedCornerShape(percent = percent.coerceIn(0f, 50f).toInt())
        style.cornerRadius != null -> RoundedCornerShape(style.cornerRadius.dp)
        fallbackPercent != null -> RoundedCornerShape(percent = fallbackPercent)
        else -> RoundedCornerShape(fallbackRadius)
    }
}

@Composable
private fun cloudBoxBackground(style: CloudBoxStyle, fallback: Color): Pair<Color, CssGradient?> {
    val alpha = (style.opacity ?: 1f).coerceIn(0f, 1f)
    val gradient = style.backgroundGradient
    val color = (style.backgroundColor?.toComposeColor() ?: fallback).copy(alpha = alpha)
    return if (gradient != null) Color.Transparent to gradient else color to null
}

@Composable
private fun cloudBoxTextStyle(
    style: CloudBoxStyle,
    baseDir: String,
    fallbackSize: TextUnit,
    fallbackColor: Color,
): TextStyle {
    val density = LocalDensity.current
    val fontFamily = rememberCssFontFamily(style.fontFamily, baseDir)
    val fontSize = style.fontSize?.let { unit ->
        if (unit.isSp) unit.value.sp else with(density) { unit.value.dp.toSp() }
    } ?: fallbackSize
    return TextStyle(
        fontSize = fontSize,
        fontFamily = fontFamily,
        color = style.color?.toComposeColor() ?: fallbackColor,
    )
}

private fun cloudBoxBorder(style: CloudBoxStyle): BorderStroke? {
    val width = style.borderWidth ?: return null
    val color = style.borderColor ?: return null
    if (width <= 0f) return null
    return BorderStroke(width.dp, color.toComposeColor())
}

private fun cloudBoxBorderModifier(style: CloudBoxStyle, shape: Shape): Modifier {
    val stroke = cloudBoxBorder(style) ?: return Modifier
    return Modifier.border(stroke, shape)
}

@Composable
private fun cloudPopupShape(style: CloudPopupStyle): Shape {
    val percent = style.cornerRadiusPercent
    return if (percent != null) {
        RoundedCornerShape(percent = percent.coerceIn(0f, 50f).toInt())
    } else {
        RoundedCornerShape(style.cornerRadius.dp)
    }
}

@Composable
private fun cloudTextStyle(style: CloudTextStyle, baseDir: String): TextStyle {
    val base = LocalTextStyle.current
    val density = LocalDensity.current
    val fontFamily = rememberCssFontFamily(style.fontFamily, baseDir)
    return remember(base, style, density, fontFamily) {
        fun CssTextUnit.toTextUnit(): TextUnit =
            if (isSp) value.sp else with(density) { value.dp.toSp() }

        val fontSize = style.fontSize?.toTextUnit()
        val effective = fontSize ?: base.fontSize.takeIf { it.isSpecified } ?: 14.sp
        val lineHeight = style.lineHeight?.let { lh ->
            if (lh.isMultiplier) effective * lh.value
            else if (lh.isSp) lh.value.sp else with(density) { lh.value.dp.toSp() }
        }
        base.copy(
            fontSize = fontSize ?: base.fontSize,
            fontWeight = style.fontWeight?.let { FontWeight(it) } ?: base.fontWeight,
            lineHeight = lineHeight ?: base.lineHeight,
            letterSpacing = style.letterSpacing?.toTextUnit() ?: base.letterSpacing,
            fontFamily = fontFamily ?: base.fontFamily,
            color = style.color?.let { it.toComposeColor() } ?: base.color,
        )
    }
}
