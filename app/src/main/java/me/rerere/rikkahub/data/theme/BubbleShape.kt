/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** 尖钩贴在气泡的哪一侧。 */
enum class TailSide { LEFT, RIGHT }

/** 尖钩沿竖直方向的位置。 */
enum class TailAnchor { TOP, CENTER, BOTTOM }

/**
 * 带小尖钩的气泡形状。
 *
 * 尖钩画在气泡本体之外，所以使用方需要在对应一侧留出 [tailSize] 的空间
 * （见 `ChatMessage` 里的 padding 处理），否则尖钩会被裁掉。
 *
 * [tailSize] 为 0 或 [side] 为 null 时退化为普通圆角矩形。
 */
data class BubbleWithTailShape(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
    /** true 表示上面四个值是百分比（0~50），按短边计算 */
    val radiusPercent: Boolean,
    val tailSize: Dp,
    /** 尖钩底边长度，null = 按 [tailSize] 推算 */
    val tailHeight: Dp?,
    val side: TailSide?,
    val anchor: TailAnchor,
    val tailOffset: Dp,
    val tailShape: TailShape,
) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rtl = layoutDirection == LayoutDirection.Rtl
        // 百分比按短边折算成像素，50% 即为全圆/胶囊
        val shortSide = minOf(size.width, size.height)
        fun Dp.resolve(): Float =
            if (radiusPercent) shortSide * (value / 100f) else with(density) { toPx() }
        // start/end 需按书写方向映射到左右
        val tl = (if (rtl) topEnd else topStart).resolve()
        val tr = (if (rtl) topStart else topEnd).resolve()
        val br = (if (rtl) bottomStart else bottomEnd).resolve()
        val bl = (if (rtl) bottomEnd else bottomStart).resolve()
        val tail = with(density) { tailSize.toPx() }

        if (side == null || tail <= 0f || size.width <= 0f || size.height <= 0f) {
            return Outline.Rounded(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    topLeft = CornerRadius(tl.clampTo(size)),
                    topRight = CornerRadius(tr.clampTo(size)),
                    bottomRight = CornerRadius(br.clampTo(size)),
                    bottomLeft = CornerRadius(bl.clampTo(size)),
                )
            )
        }

        // 气泡本体缩进一个尖钩宽度，空出的位置用来画尖钩
        val bodyLeft = if (side == TailSide.LEFT) tail else 0f
        val bodyRight = if (side == TailSide.RIGHT) size.width - tail else size.width
        if (bodyRight <= bodyLeft) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val bodyWidth = bodyRight - bodyLeft
        val maxRadius = minOf(bodyWidth, size.height) / 2f
        val rtlTop = tl.coerceIn(0f, maxRadius)
        val rtrTop = tr.coerceIn(0f, maxRadius)
        val rbr = br.coerceIn(0f, maxRadius)
        val rbl = bl.coerceIn(0f, maxRadius)

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(bodyLeft, 0f, bodyRight, size.height),
                    topLeft = CornerRadius(rtlTop),
                    topRight = CornerRadius(rtrTop),
                    bottomRight = CornerRadius(rbr),
                    bottomLeft = CornerRadius(rbl),
                )
            )
        }

        val offset = with(density) { tailOffset.toPx() }
        // 尖钩底边的中心，夹在气泡高度内避免溢出
        val halfBase = tailHeight
            ?.let { with(density) { it.toPx() } / 2f }
            ?: (tail * if (tailShape == TailShape.TRIANGLE) 1f else 1.2f)
        val anchorY = when (anchor) {
            TailAnchor.TOP -> offset + halfBase
            TailAnchor.CENTER -> size.height / 2f
            TailAnchor.BOTTOM -> size.height - offset - halfBase
        }.coerceIn(halfBase.coerceAtMost(size.height / 2f), (size.height - halfBase).coerceAtLeast(size.height / 2f))

        val tip = if (side == TailSide.LEFT) 0f else size.width
        val baseX = if (side == TailSide.LEFT) bodyLeft else bodyRight

        val tailPath = Path().apply {
            when (tailShape) {
                TailShape.TRIANGLE -> {
                    moveTo(baseX, anchorY - halfBase)
                    lineTo(tip, anchorY)
                    lineTo(baseX, anchorY + halfBase)
                }

                TailShape.RIGHT_TRIANGLE -> {
                    // 直角边贴着气泡，斜边朝外，更接近微信的尖角
                    moveTo(baseX, anchorY - halfBase)
                    lineTo(tip, anchorY - halfBase * 0.25f)
                    lineTo(baseX, anchorY + halfBase * 0.5f)
                }
            }
            close()
        }
        path.addPath(tailPath)
        return Outline.Generic(path)
    }

    private fun Float.clampTo(size: androidx.compose.ui.geometry.Size): Float =
        coerceIn(0f, minOf(size.width, size.height) / 2f)
}

/**
 * 按主题样式构造气泡形状。
 *
 * [isUser] 用于在 [TailPosition.AUTO] 时决定尖钩贴哪一侧：自己的消息贴右，对方贴左。
 */
fun BubbleStyle.toShape(isUser: Boolean): Shape {
    if (!tailVisible || tailSize <= 0f || tailPosition == TailPosition.NONE) {
        return if (radiusPercent) {
            RoundedCornerShape(
                topStartPercent = radiusTopStart.toInt(),
                topEndPercent = radiusTopEnd.toInt(),
                bottomEndPercent = radiusBottomEnd.toInt(),
                bottomStartPercent = radiusBottomStart.toInt(),
            )
        } else {
            RoundedCornerShape(
                topStart = radiusTopStart.dp,
                topEnd = radiusTopEnd.dp,
                bottomEnd = radiusBottomEnd.dp,
                bottomStart = radiusBottomStart.dp,
            )
        }
    }
    val side = tailPosition.side(isUser)
    return BubbleWithTailShape(
        topStart = radiusTopStart.dp,
        topEnd = radiusTopEnd.dp,
        bottomEnd = radiusBottomEnd.dp,
        bottomStart = radiusBottomStart.dp,
        radiusPercent = radiusPercent,
        tailSize = tailSize.dp,
        tailHeight = tailHeight?.dp,
        side = side,
        anchor = tailPosition.anchor(),
        tailOffset = tailOffset.dp,
        tailShape = tailShape,
    )
}

/** 尖钩实际占用的宽度，供调用方留白用 */
val BubbleStyle.tailSpace: Dp
    get() = if (tailVisible && tailSize > 0f && tailPosition != TailPosition.NONE) tailSize.dp else 0.dp

fun TailPosition.side(isUser: Boolean): TailSide? = when (this) {
    TailPosition.NONE -> null
    TailPosition.AUTO -> if (isUser) TailSide.RIGHT else TailSide.LEFT
    TailPosition.TOP_LEFT, TailPosition.CENTER_LEFT, TailPosition.BOTTOM_LEFT -> TailSide.LEFT
    TailPosition.TOP_RIGHT, TailPosition.CENTER_RIGHT, TailPosition.BOTTOM_RIGHT -> TailSide.RIGHT
}

fun TailPosition.anchor(): TailAnchor = when (this) {
    TailPosition.TOP_LEFT, TailPosition.TOP_RIGHT -> TailAnchor.TOP
    TailPosition.BOTTOM_LEFT, TailPosition.BOTTOM_RIGHT -> TailAnchor.BOTTOM
    // AUTO 默认贴顶，和主流聊天软件一致
    TailPosition.AUTO -> TailAnchor.TOP
    else -> TailAnchor.CENTER
}

/**
 * 头像形状。返回 null 表示沿用组件自身的默认形状
 * （圆形，加载时还有 Material 的多边形动画）。
 */
fun AvatarStyle.toShapeOrNull(): Shape? = when (shape) {
    AvatarShapeSpec.CIRCLE -> null
    AvatarShapeSpec.SQUARE -> RoundedCornerShape(0.dp)
    AvatarShapeSpec.ROUNDED -> cornerRadiusPercent?.let { RoundedCornerShape(percent = it.toInt()) }
        ?: RoundedCornerShape(cornerRadius.dp)
}

/** 把任意 [Shape] 的轮廓转成 [Path]，供阴影沿实际形状绘制。 */
private fun Shape.toPath(size: Size, layoutDirection: LayoutDirection, density: Density): Path =
    when (val outline = createOutline(size, layoutDirection, density)) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    }

/**
 * 沿 [shape] 的实际轮廓画一层阴影。
 *
 * 用自绘而非 `Modifier.shadow` / `graphicsLayer`：
 * - [BubbleWithTailShape] 把尖钩 `addPath` 并进了本体同一条 Path，
 *   取 Outline 自绘能让阴影天然带上尖钩；
 * - `Modifier.shadow` 不支持彩色和偏移，`graphicsLayer` 的阴影颜色也无法控制偏移。
 *
 * 实现上用透明画笔 + `setShadowLayer`：只落下阴影、不填充形状本身。
 * 本体范围内的阴影用 `clipOutPath` 挖掉，半透明背景底下不会透出一块深色。
 *
 * 注意：硬件加速下 `setShadowLayer` 对 `drawPath` 只在 Android 9（API 28）及以上生效，
 * 26/27 不会画出阴影——按既定方案不另做兜底，仅用 [runCatching] 保证不崩。
 *
 * 必须放在 `clip` 之前，否则超出气泡的阴影会被裁掉。
 */
fun Modifier.bubbleShadow(shadow: BubbleShadow?, shape: Shape): Modifier {
    if (shadow == null || (shadow.color ushr 24) == 0L) return this
    return drawBehind { drawBubbleShadow(shadow, shape) }
}

private fun DrawScope.drawBubbleShadow(shadow: BubbleShadow, shape: Shape) {
    if (size.minDimension <= 0f) return
    runCatching {
        val path = shape.toPath(size, layoutDirection, this).asAndroidPath()
        val spreadPx = shadow.spread.dp.toPx()
        val target = android.graphics.Path(path)
        // spread 用「以中心为锚等比放大轮廓」近似：setShadowLayer 本身没有 spread 概念
        if (spreadPx != 0f) {
            val sx = (size.width + spreadPx * 2f) / size.width
            val sy = (size.height + spreadPx * 2f) / size.height
            if (sx > 0f && sy > 0f) {
                val matrix = android.graphics.Matrix().apply {
                    setScale(sx, sy, size.width / 2f, size.height / 2f)
                }
                target.transform(matrix)
            }
        }
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                shadow.blur.dp.toPx().coerceAtLeast(0.01f),
                shadow.offsetX.dp.toPx(),
                shadow.offsetY.dp.toPx(),
                shadow.color.toInt(),
            )
        }
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            // 挖掉本体，阴影只留在轮廓外面，半透明背景不会把底下的阴影透出来
            native.clipOutPath(path)
            native.drawPath(target, paint)
            native.restore()
        }
    }
}

/**
 * 按 CSS 角度把线性渐变铺到 [shape] 的轮廓上，尖钩和气泡是同一条 Path，会一起上色。
 *
 * 0deg 向上、顺时针递增。`to top right` 这类对角写法按当前宽高比换算，
 * 让渐变轴线正好穿过两个对角。
 */
fun Modifier.bubbleGradient(gradient: CssGradient, shape: Shape, alpha: Float): Modifier {
    if (gradient.colors.size < 2 || alpha <= 0f) return this
    return drawBehind {
        if (size.minDimension <= 0f) return@drawBehind
        val angle = gradientAngleDeg(gradient, size.width, size.height)
        val rad = angle * (PI / 180.0)
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val brush = Brush.linearGradient(
            colors = gradient.colors.map { Color(it.toInt()) },
            start = center - Offset(dx, dy) * half,
            end = center + Offset(dx, dy) * half,
        )
        drawPath(
            path = shape.toPath(size, layoutDirection, this),
            brush = brush,
            alpha = alpha.coerceIn(0f, 1f),
        )
    }
}

/** 对角渐变按气泡宽高比换成 CSS 角度；普通角度原样返回。 */
private fun gradientAngleDeg(gradient: CssGradient, width: Float, height: Float): Float {
    val corner = gradient.toCorner ?: return gradient.angleDeg
    if (width <= 0f || height <= 0f) return gradient.angleDeg
    val diagonal = Math.toDegrees(atan2(height.toDouble(), width.toDouble())).toFloat()
    return when (corner) {
        GradientCorner.TOP_RIGHT -> 90f - diagonal
        GradientCorner.BOTTOM_RIGHT -> 90f + diagonal
        GradientCorner.BOTTOM_LEFT -> 270f - diagonal
        GradientCorner.TOP_LEFT -> 270f + diagonal
    }
}

/**
 * 九宫格皮肤：四个角不拉伸，四条边单向拉伸，中间双向拉伸。
 *
 * [slice] 是原图像素，顺序上右下左。[widthDp] 是切好的边在目标上画多宽（dp），
 * 为 null 时用 slice 的数值当 dp。边宽加起来超过目标时按比例缩小，避免中间被挤成负数。
 */
fun Modifier.nineSlice(image: ImageBitmap, slice: List<Float>, widthDp: List<Float>?): Modifier =
    drawBehind {
        val imageWidth = image.width
        val imageHeight = image.height
        if (imageWidth <= 0 || imageHeight <= 0 || size.minDimension <= 0f) return@drawBehind
        val srcTop = (slice.getOrElse(0) { 0f }).coerceIn(0f, imageHeight.toFloat())
        val srcRight = (slice.getOrElse(1) { 0f }).coerceIn(0f, imageWidth.toFloat())
        val srcBottom = (slice.getOrElse(2) { 0f }).coerceIn(0f, (imageHeight - srcTop).coerceAtLeast(0f))
        val srcLeft = (slice.getOrElse(3) { 0f }).coerceIn(0f, (imageWidth - srcRight).coerceAtLeast(0f))
        val widths = widthDp ?: slice
        var dstTop = (widths.getOrElse(0) { srcTop }).dp.toPx().coerceAtLeast(0f)
        var dstRight = (widths.getOrElse(1) { srcRight }).dp.toPx().coerceAtLeast(0f)
        var dstBottom = (widths.getOrElse(2) { srcBottom }).dp.toPx().coerceAtLeast(0f)
        var dstLeft = (widths.getOrElse(3) { srcLeft }).dp.toPx().coerceAtLeast(0f)
        val vertical = dstTop + dstBottom
        if (vertical > size.height && vertical > 0f) {
            val scale = size.height / vertical
            dstTop *= scale
            dstBottom *= scale
        }
        val horizontal = dstLeft + dstRight
        if (horizontal > size.width && horizontal > 0f) {
            val scale = size.width / horizontal
            dstLeft *= scale
            dstRight *= scale
        }
        val srcX = floatArrayOf(0f, srcLeft, imageWidth - srcRight)
        val srcY = floatArrayOf(0f, srcTop, imageHeight - srcBottom)
        val srcW = floatArrayOf(srcLeft, (imageWidth - srcLeft - srcRight).coerceAtLeast(0f), srcRight)
        val srcH = floatArrayOf(srcTop, (imageHeight - srcTop - srcBottom).coerceAtLeast(0f), srcBottom)
        val dstX = floatArrayOf(0f, dstLeft, size.width - dstRight)
        val dstY = floatArrayOf(0f, dstTop, size.height - dstBottom)
        val dstW = floatArrayOf(dstLeft, (size.width - dstLeft - dstRight).coerceAtLeast(0f), dstRight)
        val dstH = floatArrayOf(dstTop, (size.height - dstTop - dstBottom).coerceAtLeast(0f), dstBottom)
        for (row in 0..2) {
            for (col in 0..2) {
                val sw = srcW[col].toInt()
                val sh = srcH[row].toInt()
                val dw = dstW[col].toInt()
                val dh = dstH[row].toInt()
                if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) continue
                drawImage(
                    image = image,
                    srcOffset = IntOffset(srcX[col].toInt(), srcY[row].toInt()),
                    srcSize = IntSize(sw, sh),
                    dstOffset = IntOffset(dstX[col].toInt(), dstY[row].toInt()),
                    dstSize = IntSize(dw, dh),
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }
