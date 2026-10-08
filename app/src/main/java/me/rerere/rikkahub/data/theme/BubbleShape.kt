/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
    AvatarShapeSpec.ROUNDED -> RoundedCornerShape(cornerRadius.dp)
}
