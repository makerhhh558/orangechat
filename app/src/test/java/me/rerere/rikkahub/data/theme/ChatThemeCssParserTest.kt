/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatThemeCssParserTest {

    @Test
    fun `空输入回退内置样式`() {
        val result = ChatThemeCssParser.parse("")
        assertEquals(ChatThemeSpec.BUILTIN.userBubble, result.spec.userBubble)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.unknownProperties.isEmpty())
    }

    @Test
    fun `bubble 作用于双方气泡`() {
        val result = ChatThemeCssParser.parse(".bubble { border-radius: 20dp; }")
        assertTrue(result.errors.isEmpty())
        assertEquals(20f, result.spec.userBubble.radiusTopStart)
        assertEquals(20f, result.spec.assistantBubble.radiusTopStart)
    }

    @Test
    fun `user 与 assistant 分别生效`() {
        val css = """
            .bubble.user { background: #ff0000; }
            .bubble.assistant { background: #00ff00; }
        """.trimIndent()
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        assertEquals(0xFFFF0000L, result.spec.userBubble.backgroundColor)
        assertEquals(0xFF00FF00L, result.spec.assistantBubble.backgroundColor)
    }

    @Test
    fun `border-radius 支持 1 到 4 个值`() {
        val one = ChatThemeCssParser.parse(".bubble { border-radius: 10dp; }").spec.userBubble
        assertEquals(listOf(10f, 10f, 10f, 10f), one.radii())

        val two = ChatThemeCssParser.parse(".bubble { border-radius: 10dp 20dp; }").spec.userBubble
        // 上左/下右 = 10, 上右/下左 = 20
        assertEquals(listOf(10f, 20f, 10f, 20f), two.radii())

        val three = ChatThemeCssParser.parse(".bubble { border-radius: 10dp 20dp 30dp; }").spec.userBubble
        assertEquals(listOf(10f, 20f, 30f, 20f), three.radii())

        val four = ChatThemeCssParser.parse(".bubble { border-radius: 1dp 2dp 3dp 4dp; }").spec.userBubble
        assertEquals(listOf(1f, 2f, 3f, 4f), four.radii())
    }

    private fun BubbleStyle.radii() =
        listOf(radiusTopStart, radiusTopEnd, radiusBottomEnd, radiusBottomStart)

    @Test
    fun `颜色支持多种写法`() {
        fun color(v: String) =
            ChatThemeCssParser.parse(".bubble { background: $v; }").spec.userBubble.backgroundColor

        assertEquals(0xFFFF0000L, color("#f00"))
        assertEquals(0xFFFF0000L, color("#ff0000"))
        assertEquals(0x80FF0000L, color("#ff000080"))
        assertEquals(0xFFFF0000L, color("rgb(255, 0, 0)"))
        assertEquals(0x80FF0000L, color("rgba(255, 0, 0, 0.5019608)"))
        assertEquals(0xFFFF0000L, color("red"))
        assertEquals(0x00000000L, color("transparent"))
    }

    @Test
    fun `未知属性进 unknownProperties 而不是 errors`() {
        val result = ChatThemeCssParser.parse(".bubble {\n  box-shadow: 0 0 4px black;\n}")
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.unknownProperties.size)
        assertEquals(2, result.unknownProperties[0].line)
        assertTrue(result.unknownProperties[0].text.contains("box-shadow"))
    }

    @Test
    fun `未知选择器记为警告`() {
        val result = ChatThemeCssParser.parse(".totally-unknown { border-radius: 4dp; }")
        assertTrue(result.unknownProperties.isNotEmpty())
    }

    @Test
    fun `值非法时报行号并保留原值`() {
        val css = ".bubble {\n  border-radius: 20dp;\n  padding: abc;\n}"
        val result = ChatThemeCssParser.parse(css)
        assertEquals(1, result.errors.size)
        assertEquals(3, result.errors[0].line)
        // 同一块里合法的声明依然生效
        assertEquals(20f, result.spec.userBubble.radiusTopStart)
        // 非法值不改动默认 padding
        assertEquals(ChatThemeSpec.BUILTIN.userBubble.paddingHorizontal, result.spec.userBubble.paddingHorizontal)
    }

    @Test
    fun `缺少闭合大括号时报错且不抛异常`() {
        val result = ChatThemeCssParser.parse(".bubble { border-radius: 8dp;")
        assertTrue(result.errors.isNotEmpty())
        // 已解析到的部分仍然保留
        assertEquals(8f, result.spec.userBubble.radiusTopStart)
    }

    @Test
    fun `多余的大括号时报错`() {
        val result = ChatThemeCssParser.parse(".bubble { border-radius: 8dp; } }")
        assertTrue(result.errors.any { it.text.contains("}") })
    }

    @Test
    fun `缺少冒号的声明报错`() {
        val result = ChatThemeCssParser.parse(".bubble {\n  border-radius 8dp;\n}")
        assertTrue(result.errors.isNotEmpty())
        assertEquals(2, result.errors[0].line)
    }

    @Test
    fun `注释不影响行号`() {
        val css = "/* 第一行注释\n   跨两行 */\n.bubble {\n  nope: 1;\n}"
        val result = ChatThemeCssParser.parse(css)
        assertEquals(1, result.unknownProperties.size)
        assertEquals(4, result.unknownProperties[0].line)
    }

    @Test
    fun `尖钩属性解析`() {
        val css = """
            .bubble.user {
              tail: true;
              tail-size: 9dp;
              tail-position: bottom-right;
              tail-shape: right-triangle;
              tail-offset: 3dp;
            }
        """.trimIndent()
        val style = ChatThemeCssParser.parse(css).spec.userBubble
        assertTrue(style.tailVisible)
        assertEquals(9f, style.tailSize)
        assertEquals(TailPosition.BOTTOM_RIGHT, style.tailPosition)
        assertEquals(TailShape.RIGHT_TRIANGLE, style.tailShape)
        assertEquals(3f, style.tailOffset)
    }

    @Test
    fun `头像属性解析`() {
        val css = """
            .avatar { size: 44dp; shape: rounded; border-radius: 12dp; gap: 10dp; }
            .avatar.user { display: none; }
        """.trimIndent()
        val spec = ChatThemeCssParser.parse(css).spec
        assertEquals(44f, spec.assistantAvatar.size)
        assertEquals(AvatarShapeSpec.ROUNDED, spec.assistantAvatar.shape)
        assertEquals(12f, spec.assistantAvatar.cornerRadius)
        assertEquals(10f, spec.assistantAvatar.gap)
        assertEquals(false, spec.userAvatar.visible)
        assertNull(spec.assistantAvatar.visible)
    }

    @Test
    fun `消息列表与语音属性解析`() {
        val css = """
            .message-list { spacing: 6dp; padding-horizontal: 10dp; }
            .voice { border-radius: 24dp; bar-count: 32; bar-width: 3; waveform-color: #112233; }
        """.trimIndent()
        val spec = ChatThemeCssParser.parse(css).spec
        assertEquals(6f, spec.message.spacing)
        assertEquals(10f, spec.message.horizontalPadding)
        assertEquals(24f, spec.voice.cornerRadius)
        assertEquals(32, spec.voice.barCount)
        assertEquals(3f, spec.voice.barWidth)
        assertEquals(0xFF112233L, spec.voice.waveformColor)
    }

    @Test
    fun `max-width 百分比与小数都接受`() {
        val a = ChatThemeCssParser.parse(".bubble { max-width: 70%; }").spec.userBubble.maxWidthPercent
        val b = ChatThemeCssParser.parse(".bubble { max-width: 0.7; }").spec.userBubble.maxWidthPercent
        assertNotNull(a)
        assertEquals(0.7f, a!!, 0.001f)
        assertNotNull(b)
        assertEquals(0.7f, b!!, 0.001f)
    }

    @Test
    fun `border 简写解析出宽度和颜色`() {
        val style = ChatThemeCssParser.parse(".bubble { border: 2dp solid #0000ff; }").spec.userBubble
        assertEquals(2f, style.borderWidth)
        assertEquals(0xFF0000FFL, style.borderColor)
    }

    @Test
    fun `padding 两值分别对应纵横`() {
        val style = ChatThemeCssParser.parse(".bubble { padding: 6dp 14dp; }").spec.userBubble
        assertEquals(6f, style.paddingVertical)
        assertEquals(14f, style.paddingHorizontal)
    }

    @Test
    fun `示例主题能无错解析`() {
        val result = ChatThemeCssParser.parse(ChatThemeRepository.SAMPLE_CSS, "sample")
        assertEquals(emptyList<ChatThemeIssue>(), result.errors)
        assertEquals(emptyList<ChatThemeIssue>(), result.unknownProperties)
    }
}
