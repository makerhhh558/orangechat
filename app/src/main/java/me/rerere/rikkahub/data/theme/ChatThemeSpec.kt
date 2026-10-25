/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

/**
 * 聊天主题样式模型。
 *
 * 由 `themes/<主题名>/` 目录下的 css 文件解析而来（见 [ChatThemeCssParser]），
 * 最终被转换为 Compose 渲染参数。所有长度单位均为 dp，百分比为 0f~1f。
 *
 * 这里的默认值必须与内置默认外观一致：当 CSS 缺失、解析失败或属性写错时，
 * 回退到这些默认值即可保证界面不崩且观感不变。
 */

/** 气泡尖钩（小尾巴）位置。AUTO 表示按消息方向自动贴边。 */
enum class TailPosition {
    AUTO,
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

/** 尖钩形状。 */
enum class TailShape {
    /** 等腰三角形，微信/QQ 风格 */
    TRIANGLE,

    /** 直角三角形，贴气泡边 */
    RIGHT_TRIANGLE,
}

/** 贴纸贴在气泡的哪个角。别名规则与 [TailPosition] 一致。 */
enum class StickerCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

/**
 * 气泡贴纸，对应 css 的 `sticker` 系列属性。
 *
 * [source] 为 null 表示不显示贴纸（没写 sticker 或写了 `none`）。
 * 其余属性即使在 `sticker` 之前声明也会被记下来，所以这里整体不可为 null ——
 * css 的属性顺序不该影响结果。
 *
 * [offsetX]/[offsetY] 单位 dp，允许负值，负值让贴纸往气泡外伸。
 * [rotateRandom] 是在 [rotate] 基础上随机加减的角度范围，种子取消息 id，
 * 保证同一条消息的角度固定、滚动时不跳。
 *
 * [sourceLine] 记下 `sticker` 声明在 css 里的行号，图片找不到时报错能指到具体一行。
 */
data class BubbleSticker(
    val source: String? = null,
    val size: Float = 24f,
    val corner: StickerCorner = StickerCorner.TOP_RIGHT,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotate: Float = 0f,
    val rotateRandom: Float = 0f,
    val opacity: Float = 1f,
    /** 1 起算；0 表示没有记录到行号 */
    val sourceLine: Int = 0,
)

/** 头像形状。 */
enum class AvatarShapeSpec {
    CIRCLE,
    ROUNDED,
    SQUARE,
}

/**
 * 头像相对气泡的位置。
 * TOP = 头像与昵称在气泡上方一行（当前内置行为）；
 * SIDE = 头像与气泡并排，小手机聊天室常见布局。
 */
enum class AvatarPositionSpec {
    TOP,
    SIDE,
}

/**
 * 头像相对气泡的竖直对齐，只在 [AvatarPositionSpec.SIDE] 时生效。
 * 别名：start = TOP，middle = CENTER，end = BOTTOM。
 */
enum class AvatarAlign {
    TOP,
    CENTER,
    BOTTOM,
}

/**
 * 线性渐变，对应 css 的 `linear-gradient(...)`。
 *
 * [angleDeg] 按 CSS 约定：0 指向上方，顺时针递增。
 * [toCorner] 非 null 时表示 `to top right` 这类对角写法，渲染时按气泡宽高比换算角度。
 * [colors] 至少两个 ARGB 色，均匀分布。停靠位置（如 `red 20%`）会被忽略。
 */
data class CssGradient(
    val angleDeg: Float = 180f,
    val colors: List<Long>,
    val toCorner: GradientCorner? = null,
)

/** `to <边> <边>` 对角渐变的目标角。 */
enum class GradientCorner {
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_LEFT,
}

/**
 * 九宫格皮肤，对应 css 的 `border-image` 系列属性。
 *
 * [source] 为 null 表示没有皮肤。[disabled] 为 true 表示写了 `none`，
 * 用来覆盖同一选择器里更早的皮肤声明。
 * [slice] 是原图像素，顺序上右下左。[width] 为 null 时用 slice 的数值当 dp。
 */
data class BorderImage(
    val source: String? = null,
    val sourceLine: Int = 0,
    val disabled: Boolean = false,
    val slice: List<Float> = listOf(0f, 0f, 0f, 0f),
    val width: List<Float>? = null,
) {
    val active: Boolean get() = source != null && !disabled
}

/**
 * 气泡阴影，对应 css 的 `box-shadow`。
 *
 * [offsetX]/[offsetY]/[blur]/[spread] 单位为 dp（偏移与 spread 允许负值），
 * [color] 为 ARGB。null 表示无阴影。
 *
 * 阴影沿气泡的实际轮廓绘制（含尖钩），绘制逻辑见气泡渲染处的 `bubbleShadow`。
 */
data class BubbleShadow(
    val offsetX: Float = 0f,
    val offsetY: Float = 2f,
    val blur: Float = 6f,
    val spread: Float = 0f,
    val color: Long = 0x40000000L,
)

/**
 * 字号类长度，对应 css 的 `font-size` / `letter-spacing`。
 *
 * [isSp] 为 true 表示跟随系统字体缩放（css 写 `16sp` 或不写单位），
 * false 表示固定物理尺寸（css 写 `16dp` / `16px`）。
 */
data class CssTextUnit(
    val value: Float,
    val isSp: Boolean = true,
)

/**
 * 行高，对应 css 的 `line-height`。
 *
 * [isMultiplier] 为 true 时 [value] 是字号的倍数（css 写 `1.5`，或不带单位且不超过 4），
 * 为 false 时是长度，单位由 [isSp] 决定。不带单位且大于 4 时按 sp 处理。
 */
data class CssLineHeight(
    val value: Float,
    val isMultiplier: Boolean = false,
    val isSp: Boolean = true,
)

/**
 * 气泡正文字体，对应 css 的 `font-family`。
 *
 * 分成两种：[GENERIC] 是 serif / sans-serif / monospace 这类通用族，
 * [FILE] 是主题文件夹里的 ttf/otf。文件缺失或加载失败时回退默认字体，
 * 并由 [ChatThemeRepository] 在加载阶段报一条错误。
 */
sealed interface CssFontFamily {
    data class Generic(val name: String) : CssFontFamily
    data class File(val path: String) : CssFontFamily

    companion object {
        const val GENERIC = "generic"
        const val FILE = "file"
    }
}

/** 单个气泡的样式。 */
data class BubbleStyle(
    val radiusTopStart: Float = 16f,
    val radiusTopEnd: Float = 16f,
    val radiusBottomEnd: Float = 16f,
    val radiusBottomStart: Float = 16f,
    /** true 表示上面四个圆角值是百分比（0~100），50 即为圆形 */
    val radiusPercent: Boolean = false,
    val paddingHorizontal: Float = 8f,
    val paddingVertical: Float = 8f,
    /** null = 沿用主题色/用户在颜色设置里选的色 */
    val backgroundColor: Long? = null,
    val textColor: Long? = null,
    /** null = 不限制宽度 */
    val maxWidthPercent: Float? = null,
    val tailVisible: Boolean = false,
    /** 尖钩伸出气泡的距离（dp），对应 css 的 tail-size / tail-width */
    val tailSize: Float = 7f,
    /** 尖钩底边长度（dp），null = 按 tailSize 推算，对应 css 的 tail-height */
    val tailHeight: Float? = null,
    val tailPosition: TailPosition = TailPosition.AUTO,
    val tailShape: TailShape = TailShape.TRIANGLE,
    /** 尖钩沿气泡边缘的偏移量（dp），从所在角起算 */
    val tailOffset: Float = 14f,
    val borderWidth: Float = 0f,
    val borderColor: Long? = null,
    /** 同一条消息被拆成多个气泡时，气泡之间的间距 */
    val segmentSpacing: Float = 4f,
    /** null = 无阴影 */
    val shadow: BubbleShadow? = null,
    /** 以下字体相关属性只影响气泡正文，不影响昵称、时间等。null = 沿用应用默认 */
    val fontSize: CssTextUnit? = null,
    /** 100~900，对应 css 的 font-weight */
    val fontWeight: Int? = null,
    val lineHeight: CssLineHeight? = null,
    val letterSpacing: CssTextUnit? = null,
    val fontFamily: CssFontFamily? = null,
    /** 正文里链接的颜色 */
    val linkColor: Long? = null,
    /** 行内代码和代码块的底色 */
    val codeBackground: Long? = null,
    /** 气泡角上的贴纸，source 为 null 表示不显示 */
    val sticker: BubbleSticker = BubbleSticker(),
    /**
     * 气泡背景渐变。非 null 时盖过 [backgroundColor]。
     * 尖钩和气泡是同一条轮廓，渐变会一起铺上去。
     */
    val backgroundGradient: CssGradient? = null,
    /** 只作用于气泡背景（含渐变），不影响文字和贴纸。0~1 */
    val backgroundOpacity: Float = 1f,
    /** 九宫格皮肤。加载成功时不画背景、边框、阴影，并强制关掉尖钩 */
    val borderImage: BorderImage = BorderImage(),
)

/** 头像样式。 */
data class AvatarStyle(
    val size: Float = 36f,
    val shape: AvatarShapeSpec = AvatarShapeSpec.CIRCLE,
    /** 仅 shape=rounded 且 [cornerRadiusPercent] 为 null 时生效，单位 dp */
    val cornerRadius: Float = 10f,
    /**
     * 圆角占短边的百分比（0~50）。非 null 时盖过 [cornerRadius]。
     * css 写 `border-radius: 50%` 及以上时直接变成圆形，这里为 null。
     */
    val cornerRadiusPercent: Float? = null,
    /** 头像与气泡/昵称之间的间距 */
    val gap: Float = 8f,
    val position: AvatarPositionSpec = AvatarPositionSpec.TOP,
    /** null = 沿用显示设置里的开关 */
    val visible: Boolean? = null,
    val borderWidth: Float = 0f,
    val borderColor: Long? = null,
    /**
     * 头像挂件图片：本地相对路径或 http(s) 地址。
     * null 表示 css 没写挂件，渲染时回退到「显示设置」里的挂件。
     */
    val frameSource: String? = null,
    /**
     * css 写了 `avatar-frame: none`：连「显示设置」里的挂件也一起关掉。
     * 和 [frameSource] 为 null（没写，走兜底）是两回事。
     */
    val frameDisabled: Boolean = false,
    /** [frameSource] 所在的 css 行号，0 表示没有行号 */
    val frameSourceLine: Int = 0,
    /**
     * 挂件相对头像的大小（百分比，1.4 = 140%）。
     * null 表示没写百分比；渲染时默认按 140% 处理。
     * 和 [frameSizeDp] 互斥，后写的覆盖先写的。
     */
    val frameSizePercent: Float? = null,
    /** 挂件的绝对尺寸（dp），null 表示按百分比算 */
    val frameSizeDp: Float? = null,
    /** 挂件相对头像中心的偏移（dp），可负 */
    val frameOffsetX: Float = 0f,
    val frameOffsetY: Float = 0f,
    /** 只在 position 为 side 时生效 */
    val align: AvatarAlign = AvatarAlign.TOP,
)

/** 消息列表整体布局。 */
data class MessageStyle(
    /** 相邻消息之间的间距 */
    val spacing: Float = 12f,
    /** 列表左右内边距 */
    val horizontalPadding: Float = 16f,
)

/** 语音消息气泡样式。 */
data class VoiceStyle(
    val cornerRadius: Float = 16f,
    /** 圆角占短边的百分比（0~50）。非 null 时盖过 [cornerRadius] */
    val cornerRadiusPercent: Float? = null,
    val backgroundColor: Long? = null,
    /** 非 null 时盖过 [backgroundColor] */
    val backgroundGradient: CssGradient? = null,
    /** 只作用于背景（含渐变），不影响文字、波形和贴纸。0~1 */
    val backgroundOpacity: Float = 1f,
    /** 秒数和转写文字的颜色。null 时用主题默认色 */
    val textColor: Long? = null,
    val waveformColor: Long? = null,
    val shadow: BubbleShadow? = null,
    val borderWidth: Float = 0f,
    val borderColor: Long? = null,
    /** 角上的贴纸，行为和气泡的 [BubbleStyle.sticker] 一样 */
    val sticker: BubbleSticker = BubbleSticker(),
    val iconSize: Float = 24f,
    val waveformWidth: Float = 60f,
    val waveformHeight: Float = 24f,
    val barCount: Int = 24,
    val barWidth: Float = 2.5f,
    val paddingHorizontal: Float = 12f,
    val paddingVertical: Float = 8f,
    /** 九宫格皮肤，行为和气泡的 [BubbleStyle.borderImage] 一样 */
    val borderImage: BorderImage = BorderImage(),
)

/**
 * ☁️ 图标样式，对应 `.cloud-icon`。
 * [imageSource] 为 null 时用默认的 ☁️ 字符。
 */
data class CloudIconStyle(
    val imageSource: String? = null,
    /** 1 起算；0 表示没有记录到行号 */
    val imageSourceLine: Int = 0,
    /** 图标边长（dp） */
    val size: Float = 20f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val opacity: Float = 1f,
)

/**
 * ☁️ 弹窗样式，对应 `.cloud-popup`。
 * 只用解析器已经支持的属性，贴纸叠在内容上层。
 */
data class CloudPopupStyle(
    val cornerRadius: Float = 12f,
    /** 圆角占短边的百分比（0~50）。非 null 时盖过 [cornerRadius] */
    val cornerRadiusPercent: Float? = null,
    val backgroundColor: Long? = null,
    val backgroundGradient: CssGradient? = null,
    /** 只作用于背景，不影响文字和贴纸。0~1 */
    val backgroundOpacity: Float = 1f,
    val borderWidth: Float = 0f,
    val borderColor: Long? = null,
    val shadow: BubbleShadow? = null,
    val paddingHorizontal: Float = 12f,
    val paddingVertical: Float = 8f,
    /** 弹窗最大宽度（dp） */
    val maxWidth: Float = 240f,
    val sticker: BubbleSticker = BubbleSticker(),
)

/**
 * ☁️ 面板里的盒子样式，心情胶囊、进度条轨道和数值共用。
 * 字段全是 null，表示没写这条 CSS，渲染时用内置的素净默认。
 * [mergeOver] 用来做分档回落：分档里写了的字段盖过基础样式。
 */
data class CloudBoxStyle(
    val backgroundColor: Long? = null,
    val backgroundGradient: CssGradient? = null,
    val opacity: Float? = null,
    val cornerRadius: Float? = null,
    /** 圆角占短边的百分比（0~50）。非 null 时盖过 [cornerRadius] */
    val cornerRadiusPercent: Float? = null,
    val paddingHorizontal: Float? = null,
    val paddingVertical: Float? = null,
    val borderWidth: Float? = null,
    val borderColor: Long? = null,
    val shadow: BubbleShadow? = null,
    /** 轨道高度（dp），进度条用 */
    val height: Float? = null,
    val fontSize: CssTextUnit? = null,
    val fontFamily: CssFontFamily? = null,
    val color: Long? = null,
) {
    /** 自身非 null 的字段盖过 [base]，没写的沿用 [base]。 */
    fun mergeOver(base: CloudBoxStyle): CloudBoxStyle = CloudBoxStyle(
        backgroundColor = backgroundColor ?: base.backgroundColor,
        backgroundGradient = backgroundGradient ?: base.backgroundGradient,
        opacity = opacity ?: base.opacity,
        cornerRadius = cornerRadius ?: base.cornerRadius,
        cornerRadiusPercent = cornerRadiusPercent ?: base.cornerRadiusPercent,
        paddingHorizontal = paddingHorizontal ?: base.paddingHorizontal,
        paddingVertical = paddingVertical ?: base.paddingVertical,
        borderWidth = borderWidth ?: base.borderWidth,
        borderColor = borderColor ?: base.borderColor,
        shadow = shadow ?: base.shadow,
        height = height ?: base.height,
        fontSize = fontSize ?: base.fontSize,
        fontFamily = fontFamily ?: base.fontFamily,
        color = color ?: base.color,
    )
}

/** ☁️ 弹窗文字样式，对应 `.cloud-text`。null 表示沿用应用默认。 */
data class CloudTextStyle(
    val fontSize: CssTextUnit? = null,
    /** 100~900 */
    val fontWeight: Int? = null,
    val lineHeight: CssLineHeight? = null,
    val letterSpacing: CssTextUnit? = null,
    val fontFamily: CssFontFamily? = null,
    val color: Long? = null,
)

/**
 * 一套完整的聊天主题。
 *
 * [BUILTIN] 是内置默认样式，也是所有回退路径的终点。
 */
data class ChatThemeSpec(
    val name: String = "",
    /**
     * 主题文件夹的绝对路径，供 css 里的相对路径（字体、图片）定位用。
     * 空字符串表示内置主题 / 无文件来源，此时相对路径一律视为找不到。
     */
    val baseDir: String = "",
    val userBubble: BubbleStyle = BubbleStyle(),
    val assistantBubble: BubbleStyle = BubbleStyle(),
    val userAvatar: AvatarStyle = AvatarStyle(),
    val assistantAvatar: AvatarStyle = AvatarStyle(size = 32f),
    val message: MessageStyle = MessageStyle(),
    val userVoice: VoiceStyle = VoiceStyle(),
    val assistantVoice: VoiceStyle = VoiceStyle(),
    val cloudIcon: CloudIconStyle = CloudIconStyle(),
    val cloudPopup: CloudPopupStyle = CloudPopupStyle(),
    val cloudText: CloudTextStyle = CloudTextStyle(),
    val cloudMood: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarRow: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarLabel: CloudBoxStyle = CloudBoxStyle(),
    val cloudBar: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarFill: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarFillLow: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarFillMid: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarFillHigh: CloudBoxStyle = CloudBoxStyle(),
    val cloudBarValue: CloudBoxStyle = CloudBoxStyle(),
) {
    companion object {
        /** 内置默认样式，与改造前的界面观感保持一致 */
        val BUILTIN = ChatThemeSpec()
    }
}

/**
 * 四角统一圆角。
 * 未启用 CSS 主题时用来接上"显示设置"里原有的气泡圆角滑块，保持既有行为。
 */
fun BubbleStyle.withUniformRadius(radius: Float): BubbleStyle = copy(
    radiusTopStart = radius,
    radiusTopEnd = radius,
    radiusBottomEnd = radius,
    radiusBottomStart = radius,
    radiusPercent = false,
)

/** 解析过程中的一条问题记录。 */
data class ChatThemeIssue(
    /** 1 起算的行号，0 表示无法定位到具体行 */
    val line: Int,
    val text: String,
) {
    override fun toString(): String = if (line > 0) "第 $line 行: $text" else text
}

/**
 * CSS 解析结果。
 *
 * 即使 [errors] 非空，[spec] 也始终可用。回退粒度固定为"只丢弃出错的那一条声明"：
 * 值写错的声明被跳过，同一个块里其余声明照常生效；
 * 只有在解析器自身抛出异常（文件损坏等彻底无法解析的情况）时才整体回退到内置默认样式。
 */
data class ChatThemeParseResult(
    val spec: ChatThemeSpec,
    /** 语法错误、值非法等需要让用户知道的问题 */
    val errors: List<ChatThemeIssue> = emptyList(),
    /** 不认识的选择器/属性：静默忽略，但列出来方便用户知道哪些没生效 */
    val unknownProperties: List<ChatThemeIssue> = emptyList(),
) {
    val hasProblem: Boolean
        get() = errors.isNotEmpty() || unknownProperties.isNotEmpty()
}
