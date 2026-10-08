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
)

/** 头像样式。 */
data class AvatarStyle(
    val size: Float = 36f,
    val shape: AvatarShapeSpec = AvatarShapeSpec.CIRCLE,
    /** 仅 shape=rounded 时生效 */
    val cornerRadius: Float = 10f,
    /** 头像与气泡/昵称之间的间距 */
    val gap: Float = 8f,
    val position: AvatarPositionSpec = AvatarPositionSpec.TOP,
    /** null = 沿用显示设置里的开关 */
    val visible: Boolean? = null,
    val borderWidth: Float = 0f,
    val borderColor: Long? = null,
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
    val backgroundColor: Long? = null,
    val waveformColor: Long? = null,
    val iconSize: Float = 24f,
    val waveformWidth: Float = 60f,
    val waveformHeight: Float = 24f,
    val barCount: Int = 24,
    val barWidth: Float = 2.5f,
    val paddingHorizontal: Float = 12f,
    val paddingVertical: Float = 8f,
)

/**
 * 一套完整的聊天主题。
 *
 * [BUILTIN] 是内置默认样式，也是所有回退路径的终点。
 */
data class ChatThemeSpec(
    val name: String = "",
    val userBubble: BubbleStyle = BubbleStyle(),
    val assistantBubble: BubbleStyle = BubbleStyle(),
    val userAvatar: AvatarStyle = AvatarStyle(),
    val assistantAvatar: AvatarStyle = AvatarStyle(size = 32f),
    val message: MessageStyle = MessageStyle(),
    val voice: VoiceStyle = VoiceStyle(),
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
