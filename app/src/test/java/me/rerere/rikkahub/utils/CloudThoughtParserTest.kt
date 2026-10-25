/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudThoughtParserTest {

    @Test
    fun `正常标签`() {
        val result = parseCloudThought("<cloud>有点开心</cloud>\n你好呀")
        assertEquals("有点开心", result.cloud)
        assertEquals("你好呀", result.body)
    }

    @Test
    fun `没有标签`() {
        val result = parseCloudThought("你好呀")
        assertNull(result.cloud)
        assertEquals("你好呀", result.body)
    }

    @Test
    fun `只有开头没闭合`() {
        val result = parseCloudThought("你好<cloud>还在想")
        assertNull(result.cloud)
        assertEquals("你好", result.body)
    }

    @Test
    fun `空标签`() {
        val result = parseCloudThought("<cloud>   </cloud>你好")
        assertNull(result.cloud)
        assertEquals("你好", result.body)
    }

    @Test
    fun `多个标签只取第一段并全部删除`() {
        val result = parseCloudThought("<cloud>第一段</cloud>正文<cloud>第二段</cloud>结尾")
        assertEquals("第一段", result.cloud)
        assertEquals("正文结尾", result.body)
    }

    @Test
    fun `cloud 内的换行不进正文`() {
        val result = parseCloudThought("<cloud>第一行\n第二行</cloud>\n正文第一行\n正文第二行")
        assertEquals("第一行\n第二行", result.cloud)
        assertEquals("正文第一行\n正文第二行", result.body)
    }

    @Test
    fun `标签前后的空白`() {
        val result = parseCloudThought("  \n<cloud>  心里话  </cloud>\n\n  正文")
        assertEquals("心里话", result.cloud)
        assertEquals("正文", result.body)
    }

    @Test
    fun `大小写都认`() {
        val result = parseCloudThought("<CLOUD>大写</Cloud>\n正文")
        assertEquals("大写", result.cloud)
        assertEquals("正文", result.body)
    }

    @Test
    fun `流式输出标签被切在两个 chunk 中间`() {
        val chunks = listOf("开头<clo", "ud>心里", "话</clo", "ud>\n正文")
        val stages = chunks.runningFold("") { acc, chunk -> acc + chunk }.drop(1).map { parseCloudThought(it) }

        // "<clo" 还没成标签，先藏起来
        assertNull(stages[0].cloud)
        assertEquals("开头", stages[0].body)
        // 开了头但没闭合，整段不显示
        assertNull(stages[1].cloud)
        assertEquals("开头", stages[1].body)
        // 闭合标签也只到一半
        assertNull(stages[2].cloud)
        assertEquals("开头", stages[2].body)
        // 闭合后才出云朵；标签前已经显示的文字保留，正文从标签后面接上
        assertEquals("心里话", stages[3].cloud)
        assertEquals("开头\n正文", stages[3].body)
    }

    @Test
    fun `残缺的闭合标签不露出来`() {
        val result = parseCloudThought("正文</clo")
        assertNull(result.cloud)
        assertEquals("正文", result.body)
    }

    @Test
    fun `正文里的换行保留给气泡切分`() {
        val result = parseCloudThought("<cloud>嗯</cloud>第一句\n\n第二句")
        assertEquals("嗯", result.cloud)
        assertEquals("第一句\n\n第二句", result.body)
    }

    @Test
    fun `正常心情胶囊和进度条`() {
        val blocks = parseCloudContent("<mood>小得意</mood>\n<bar label=\"好奇\" value=\"92\"/>")
        assertEquals(listOf(CloudBlock.Mood("小得意"), CloudBlock.Bar("好奇", 92f)), blocks)
    }

    @Test
    fun `进度条数值夹取`() {
        assertEquals(100f, barValue("<bar label=\"a\" value=\"150\"/>"))
        assertEquals(0f, barValue("<bar label=\"a\" value=\"-5\"/>"))
        assertEquals(92.5f, barValue("<bar label=\"a\" value=\"92.5\"/>"))
    }

    @Test
    fun `非法进度条被丢掉且原文不外露`() {
        listOf(
            "<bar label=\"好奇\" value=\"很多\"/>",
            "<bar value=\"92\"/>",
            "<bar label=\"好奇\" value=\"92\"",
            "<mood>没闭合",
        ).forEach { raw ->
            val blocks = parseCloudContent("前文${raw}后文")
            assertTrue(blocks.none { it is CloudBlock.Bar || it is CloudBlock.Mood })
            val text = blocks.filterIsInstance<CloudBlock.Markdown>().joinToString("") { it.text }
            assertFalse(text.contains("<bar"))
            assertFalse(text.contains("<mood"))
        }
    }

    @Test
    fun `单引号颠倒顺序和成对写法`() {
        assertEquals(
            CloudBlock.Bar("好奇", 40f),
            parseCloudContent("<bar value='40' label='好奇'/>").single(),
        )
        assertEquals(
            CloudBlock.Bar("开心", 60f),
            parseCloudContent("<bar label=\"开心\" value=\"60\"></bar>").single(),
        )
        val paired = parseCloudContent("<bar label=\"想她\" value=\"92\">中间不露</bar>")
        assertEquals(listOf(CloudBlock.Bar("想她", 92f)), paired)
    }

    @Test
    fun `进度条分档边界`() {
        assertEquals(CloudBarTier.LOW, cloudBarTier(40f))
        assertEquals(CloudBarTier.MID, cloudBarTier(41f))
        assertEquals(CloudBarTier.MID, cloudBarTier(75f))
        assertEquals(CloudBarTier.HIGH, cloudBarTier(76f))
    }

    @Test
    fun `文字心情和进度条混排保持顺序`() {
        val blocks = parseCloudContent("前文<mood>小得意</mood>后文\n<bar label=\"好奇\" value=\"30\"/>")
        assertEquals(
            listOf(
                CloudBlock.Markdown("前文"),
                CloudBlock.Mood("小得意"),
                CloudBlock.Markdown("后文"),
                CloudBlock.Bar("好奇", 30f),
            ),
            blocks,
        )
    }

    @Test
    fun `没有自定义标签时原样返回`() {
        val raw = "一句普通文字\n`小得意` **粗体**"
        assertEquals(listOf(CloudBlock.Markdown(raw)), parseCloudContent(raw))
    }

    @Test
    fun `空心情胶囊丢掉`() {
        assertEquals(listOf(CloudBlock.Markdown("只剩文字")), parseCloudContent("<mood>  </mood>只剩文字"))
    }

    private fun barValue(raw: String): Float = (parseCloudContent(raw).single() as CloudBlock.Bar).value
}
