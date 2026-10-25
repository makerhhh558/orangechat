/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.prompts

val DEFAULT_CLOUD_PROMPT = """
    在回复末尾用 <cloud>…</cloud> 写下你此刻自己的内心感受，只写你自己的情绪和想法，不要写对用户的分析、观察或打算（比如“记一下她喜欢这个”“别用太频繁”这类都不要）。称呼对方按人设里的称呼来，不要默认用“他”。
    可以用 <mood>心情词</mood> 写一个此刻的心情，用 <bar label="情绪名" value="0到100的数字"/> 写一两个情绪值。内容简短，三五行以内。
""".trimIndent()
