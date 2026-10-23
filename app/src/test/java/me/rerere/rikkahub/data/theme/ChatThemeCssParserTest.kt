/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val result = ChatThemeCssParser.parse(".bubble {\n  backdrop-filter: blur(4dp);\n}")
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.unknownProperties.size)
        assertEquals(2, result.unknownProperties[0].line)
        assertTrue(result.unknownProperties[0].text.contains("backdrop-filter"))
    }

    @Test
    fun `贴纸七个属性都能解析`() {
        val css = """
            .bubble.assistant {
                background: #ffffff;
                sticker: url("cat.png");
                sticker-size: 30dp;
                sticker-position: top-right;
                sticker-offset: -9dp -9dp;     /* 负值＝往气泡外伸 */
                sticker-rotate: 8deg;
                sticker-rotate-random: 6deg;
                sticker-opacity: 0.95;
            }
        """.trimIndent()
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        val sticker = result.spec.assistantBubble.sticker
        assertEquals("cat.png", sticker.source)
        assertEquals(30f, sticker.size)
        assertEquals(StickerCorner.TOP_RIGHT, sticker.corner)
        assertEquals(-9f, sticker.offsetX)
        assertEquals(-9f, sticker.offsetY)
        assertEquals(8f, sticker.rotate)
        assertEquals(6f, sticker.rotateRandom)
        assertEquals(0.95f, sticker.opacity)
        // sticker 声明在第 3 行，行号要记下来供"图片找不到"报错定位
        assertEquals(3, sticker.sourceLine)
        // 贴纸只配在 assistant 上，不该漏到 user
        assertNull(result.spec.userBubble.sticker.source)
    }

    @Test
    fun `贴纸四个角与反写别名都能解析`() {
        val cases = mapOf(
            "top-left" to StickerCorner.TOP_LEFT,
            "left-top" to StickerCorner.TOP_LEFT,
            "top-right" to StickerCorner.TOP_RIGHT,
            "right-top" to StickerCorner.TOP_RIGHT,
            "bottom-left" to StickerCorner.BOTTOM_LEFT,
            "left-bottom" to StickerCorner.BOTTOM_LEFT,
            "bottom-right" to StickerCorner.BOTTOM_RIGHT,
            "right-bottom" to StickerCorner.BOTTOM_RIGHT,
            // 大小写和下划线都不挑
            "TOP_LEFT" to StickerCorner.TOP_LEFT,
            "Bottom_Right" to StickerCorner.BOTTOM_RIGHT,
        )
        cases.forEach { (value, expected) ->
            val result = ChatThemeCssParser.parse(".bubble { sticker-position: $value; }")
            assertTrue("$value 不该报错", result.errors.isEmpty())
            assertEquals(value, expected, result.spec.userBubble.sticker.corner)
        }

        // 认不出来的值只跳过这一条并带行号报错
        val bad = ChatThemeCssParser.parse(".bubble {\n  sticker-position: middle;\n}")
        assertEquals(1, bad.errors.size)
        assertEquals(2, bad.errors[0].line)
        assertEquals(StickerCorner.TOP_RIGHT, bad.spec.userBubble.sticker.corner)
    }

    @Test
    fun `sticker none 不留行号`() {
        val result = ChatThemeCssParser.parse(".bubble {\n  sticker: url(\"a.png\");\n  sticker: none;\n}")
        assertTrue(result.errors.isEmpty())
        assertNull(result.spec.userBubble.sticker.source)
        assertEquals(0, result.spec.userBubble.sticker.sourceLine)
    }

    @Test
    fun `贴纸支持远程地址且注释不吃掉双斜杠`() {
        val css = ".bubble { sticker: url(\"https://example.com/a.png\"); }"
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        assertEquals("https://example.com/a.png", result.spec.userBubble.sticker.source)
    }

    @Test
    fun `贴纸属性顺序不影响结果`() {
        // sticker 写在其它 sticker-xxx 之后也要生效
        val css = ".bubble { sticker-size: 40dp; sticker: url(\"a.png\"); }"
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        assertEquals("a.png", result.spec.userBubble.sticker.source)
        assertEquals(40f, result.spec.userBubble.sticker.size)
    }

    @Test
    fun `贴纸 none 与非法值`() {
        val none = ChatThemeCssParser.parse(".bubble { sticker: none; }")
        assertTrue(none.errors.isEmpty())
        assertNull(none.spec.userBubble.sticker.source)

        val bad = ChatThemeCssParser.parse(".bubble {\n  sticker-size: abc;\n}")
        assertEquals(1, bad.errors.size)
        assertEquals(2, bad.errors[0].line)
        // 非法值不改动默认尺寸
        assertEquals(24f, bad.spec.userBubble.sticker.size)
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
    fun `头像挂件默认不启用`() {
        val avatar = ChatThemeCssParser.parse("").spec.userAvatar
        assertNull(avatar.frameSource)
        assertFalse(avatar.frameDisabled)
        assertNull(avatar.frameSizePercent)
        assertNull(avatar.frameSizeDp)
        assertEquals(0f, avatar.frameOffsetX)
        assertEquals(0f, avatar.frameOffsetY)
    }

    @Test
    fun `头像挂件百分比尺寸与偏移`() {
        val css = """
            .avatar {
              avatar-frame: url("frame.png");
              avatar-frame-size: 140%;
              avatar-frame-offset: -2dp 4dp;
            }
        """.trimIndent()
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        val avatar = result.spec.userAvatar
        assertEquals("frame.png", avatar.frameSource)
        assertFalse(avatar.frameDisabled)
        assertEquals(2, avatar.frameSourceLine)
        assertEquals(1.4f, avatar.frameSizePercent!!, 0.001f)
        assertNull(avatar.frameSizeDp)
        assertEquals(-2f, avatar.frameOffsetX)
        assertEquals(4f, avatar.frameOffsetY)
        // .avatar 同时作用于助手侧
        assertEquals("frame.png", result.spec.assistantAvatar.frameSource)
    }

    @Test
    fun `头像挂件绝对尺寸覆盖百分比`() {
        val css = """
            .avatar.user {
              avatar-frame-size: 180%;
              avatar-frame-size: 48dp;
            }
        """.trimIndent()
        val avatar = ChatThemeCssParser.parse(css).spec.userAvatar
        assertEquals(48f, avatar.frameSizeDp)
        assertNull(avatar.frameSizePercent)
        // 只覆盖用户侧
        assertNull(ChatThemeCssParser.parse(css).spec.assistantAvatar.frameSizeDp)
    }

    @Test
    fun `头像挂件 none 关闭并清空来源`() {
        val css = """
            .avatar { avatar-frame: url("frame.png"); }
            .avatar.assistant { avatar-frame: none; }
        """.trimIndent()
        val spec = ChatThemeCssParser.parse(css).spec
        assertEquals("frame.png", spec.userAvatar.frameSource)
        assertFalse(spec.userAvatar.frameDisabled)
        assertNull(spec.assistantAvatar.frameSource)
        assertTrue(spec.assistantAvatar.frameDisabled)
        assertEquals(0, spec.assistantAvatar.frameSourceLine)
    }

    @Test
    fun `头像挂件非法值报行号且不改原值`() {
        val css = ".avatar {\n  avatar-frame: url();\n  avatar-frame-size: 0%;\n  avatar-frame-offset: abc;\n}"
        val result = ChatThemeCssParser.parse(css)
        assertEquals(3, result.errors.size)
        assertEquals(2, result.errors[0].line)
        val avatar = result.spec.userAvatar
        assertNull(avatar.frameSource)
        assertFalse(avatar.frameDisabled)
        assertNull(avatar.frameSizePercent)
        assertEquals(0f, avatar.frameOffsetX)
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
        assertEquals(24f, spec.userVoice.cornerRadius)
        assertEquals(32, spec.userVoice.barCount)
        assertEquals(3f, spec.userVoice.barWidth)
        assertEquals(0xFF112233L, spec.userVoice.waveformColor)
        assertEquals(24f, spec.assistantVoice.cornerRadius)
        assertEquals(32, spec.assistantVoice.barCount)
    }

    @Test
    fun `气泡渐变解析角度和方向`() {
        val deg = ChatThemeCssParser.parse(
            ".bubble.user { background: linear-gradient(45deg, #ff0000, #00ff00); }"
        ).spec.userBubble
        val gradient = deg.backgroundGradient
        assertNotNull(gradient)
        assertEquals(45f, gradient!!.angleDeg)
        assertNull(gradient.toCorner)
        assertEquals(listOf(0xFFFF0000L, 0xFF00FF00L), gradient.colors)
        assertNull(deg.backgroundColor)

        val right = ChatThemeCssParser.parse(
            ".bubble { background: linear-gradient(to right, red, blue); }"
        ).spec.userBubble.backgroundGradient!!
        assertEquals(90f, right.angleDeg)
        assertNull(right.toCorner)

        val corner = ChatThemeCssParser.parse(
            ".bubble { background: linear-gradient(to top right, red, blue); }"
        ).spec.userBubble.backgroundGradient!!
        assertEquals(45f, corner.angleDeg)
        assertEquals(GradientCorner.TOP_RIGHT, corner.toCorner)

        val plain = ChatThemeCssParser.parse(
            ".bubble { background: linear-gradient(red, blue); }"
        ).spec.userBubble.backgroundGradient!!
        assertEquals(180f, plain.angleDeg)
        assertNull(plain.toCorner)
    }

    @Test
    fun `渐变非法值报错且纯色会清掉渐变`() {
        val bad = ChatThemeCssParser.parse(".bubble {\n  background: linear-gradient(45deg, red);\n}")
        assertEquals(1, bad.errors.size)
        assertEquals(2, bad.errors[0].line)
        assertNull(bad.spec.userBubble.backgroundGradient)

        val css = """
            .bubble { background: linear-gradient(to bottom, red, blue); }
            .bubble.user { background: #112233; }
        """.trimIndent()
        val result = ChatThemeCssParser.parse(css)
        assertNull(result.spec.userBubble.backgroundGradient)
        assertEquals(0xFF112233L, result.spec.userBubble.backgroundColor)
        assertNotNull(result.spec.assistantBubble.backgroundGradient)
    }

    @Test
    fun `气泡透明度`() {
        assertEquals(0.5f, ChatThemeCssParser.parse(".bubble.user { opacity: 0.5; }").spec.userBubble.backgroundOpacity)
        assertEquals(0.5f, ChatThemeCssParser.parse(".bubble { opacity: 50%; }").spec.assistantBubble.backgroundOpacity)
        val bad = ChatThemeCssParser.parse(".bubble {\n  opacity: 2;\n}")
        assertEquals(1, bad.errors.size)
        assertEquals(2, bad.errors[0].line)
        assertEquals(1f, bad.spec.userBubble.backgroundOpacity)
    }

    @Test
    fun `九宫格皮肤解析 slice 和 width`() {
        val css = """
            .bubble.user {
              border-image: url("skin.png");
              border-image-slice: 10 20 30 40;
              border-image-width: 8dp 12dp;
            }
            .bubble.assistant { border-image: none; }
        """.trimIndent()
        val spec = ChatThemeCssParser.parse(css).spec
        val user = spec.userBubble.borderImage
        assertEquals("skin.png", user.source)
        assertFalse(user.disabled)
        assertEquals(listOf(10f, 20f, 30f, 40f), user.slice)
        assertEquals(listOf(8f, 12f, 8f, 12f), user.width)
        assertTrue(spec.assistantBubble.borderImage.disabled)
        assertNull(spec.assistantBubble.borderImage.source)
    }

    @Test
    fun `九宫格皮肤非法值带行号`() {
        val css = ".bubble {\n  border-image: url();\n  border-image-slice: -1;\n  border-image-width: abc;\n}"
        val result = ChatThemeCssParser.parse(css)
        assertEquals(3, result.errors.size)
        assertEquals(2, result.errors[0].line)
        val image = result.spec.userBubble.borderImage
        assertNull(image.source)
        assertEquals(listOf(0f, 0f, 0f, 0f), image.slice)
        assertNull(image.width)
    }

    @Test
    fun `头像对齐只改对应侧`() {
        val css = """
            .avatar { avatar-align: bottom; }
            .avatar.user { align: center; }
            .avatar.assistant { avatar-align: start; }
        """.trimIndent()
        val spec = ChatThemeCssParser.parse(css).spec
        assertEquals(AvatarAlign.CENTER, spec.userAvatar.align)
        assertEquals(AvatarAlign.TOP, spec.assistantAvatar.align)
        val bad = ChatThemeCssParser.parse(".avatar {\n  avatar-align: left;\n}")
        assertEquals(1, bad.errors.size)
        assertEquals(2, bad.errors[0].line)
        assertEquals(AvatarAlign.TOP, bad.spec.userAvatar.align)
    }

    @Test
    fun `语音两侧皮肤互不覆盖`() {
        val css = """
            .voice { border-image: url("both.png"); border-radius: 20dp; }
            .voice.user { border-image: url("me.png"); border-image-slice: 12; }
            .voice.assistant { border-image: none; }
        """.trimIndent()
        val spec = ChatThemeCssParser.parse(css).spec
        assertEquals("me.png", spec.userVoice.borderImage.source)
        assertEquals(listOf(12f, 12f, 12f, 12f), spec.userVoice.borderImage.slice)
        assertEquals(20f, spec.userVoice.cornerRadius)
        assertTrue(spec.assistantVoice.borderImage.disabled)
        assertNull(spec.assistantVoice.borderImage.source)
        assertEquals(20f, spec.assistantVoice.cornerRadius)
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
    fun `important 被剥掉且大小写和空格不影响`() {
        val css = """
            .bubble.user { background: #ff0000 !important; opacity: 0.4 ! IMPORTANT; }
            .bubble.assistant { border-radius: 12dp ! important; }
        """.trimIndent()
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        assertEquals(0xFFFF0000L, result.spec.userBubble.backgroundColor)
        assertEquals(0.4f, result.spec.userBubble.backgroundOpacity)
        assertEquals(12f, result.spec.assistantBubble.radiusTopStart)
        assertFalse(result.spec.assistantBubble.radiusPercent)
    }

    @Test
    fun `渐变停靠位置被忽略颜色仍均匀分布`() {
        val gradient = ChatThemeCssParser.parse(
            ".bubble { background: linear-gradient(90deg, red 20%, rgba(0,0,0,0.5) 80%); }"
        )
        assertTrue(gradient.errors.isEmpty())
        val colors = gradient.spec.userBubble.backgroundGradient!!.colors
        assertEquals(2, colors.size)
        assertEquals(0xFFFF0000L, colors[0])
        // 0.5 * 255 截断为 127，即 0x7F
        assertEquals(0x7F000000L, colors[1])
    }

    @Test
    fun `border-image 简写接受 fill 和宽度`() {
        val image = ChatThemeCssParser.parse(
            ".bubble { border-image: url(a.png) 30 fill / 15dp; }"
        ).spec.userBubble.borderImage
        assertEquals("a.png", image.source)
        assertEquals(listOf(30f, 30f, 30f, 30f), image.slice)
        assertEquals(listOf(15f, 15f, 15f, 15f), image.width)
        assertFalse(image.disabled)
    }

    @Test
    fun `头像和语音的百分比圆角`() {
        val avatar = ChatThemeCssParser.parse(".avatar { border-radius: 50%; }").spec.userAvatar
        assertEquals(AvatarShapeSpec.CIRCLE, avatar.shape)
        assertNull(avatar.cornerRadiusPercent)
        val rounded = ChatThemeCssParser.parse(".avatar { border-radius: 20%; }").spec.userAvatar
        assertEquals(AvatarShapeSpec.ROUNDED, rounded.shape)
        assertEquals(20f, rounded.cornerRadiusPercent)
        val voice = ChatThemeCssParser.parse(".voice { border-radius: 50%; }").spec.userVoice
        assertEquals(50f, voice.cornerRadiusPercent)
    }

    @Test
    fun `不带单位的行高超过 4 按 sp`() {
        val big = ChatThemeCssParser.parse(".bubble { line-height: 24; }").spec.userBubble.lineHeight!!
        assertFalse(big.isMultiplier)
        assertTrue(big.isSp)
        assertEquals(24f, big.value)
        val small = ChatThemeCssParser.parse(".bubble { line-height: 1.5; }").spec.userBubble.lineHeight!!
        assertTrue(small.isMultiplier)
        assertEquals(1.5f, small.value)
    }

    @Test
    fun `语音补齐文字色透明度和贴纸且 color 仍是波形色`() {
        val css = """
            .voice.user {
              color: #112233;
              text-color: #abcdef;
              opacity: 0.5;
              box-shadow: 0 2dp 6dp #000000;
              sticker: url("cat.png");
              sticker-position: bottom-left;
            }
        """.trimIndent()
        val result = ChatThemeCssParser.parse(css)
        assertTrue(result.errors.isEmpty())
        val voice = result.spec.userVoice
        assertEquals(0xFF112233L, voice.waveformColor)
        assertEquals(0xFFABCDEFL, voice.textColor)
        assertEquals(0.5f, voice.backgroundOpacity)
        assertNotNull(voice.shadow)
        assertEquals("cat.png", voice.sticker.source)
        assertEquals(StickerCorner.BOTTOM_LEFT, voice.sticker.corner)
        assertNull(result.spec.assistantVoice.sticker.source)
    }

    @Test
    fun `padding 四值只取前两个`() {
        val style = ChatThemeCssParser.parse(
            ".bubble { padding: 8px 12px 10px 12px; }"
        ).spec.userBubble
        assertEquals(8f, style.paddingVertical)
        assertEquals(12f, style.paddingHorizontal)
    }

    @Test
    fun `示例主题能无错解析`() {
        val result = ChatThemeCssParser.parse(ChatThemeRepository.SAMPLE_CSS, "sample")
        assertEquals(emptyList<ChatThemeIssue>(), result.errors)
        assertEquals(emptyList<ChatThemeIssue>(), result.unknownProperties)
    }
}
