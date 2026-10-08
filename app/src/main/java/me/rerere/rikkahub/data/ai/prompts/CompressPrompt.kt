/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.prompts

internal val DEFAULT_COMPRESS_PROMPT = """
    You are a conversation compression assistant. Compress the following conversation into a concise summary.

    Requirements:
    1. Preserve key facts, decisions, and important context that would be needed to continue the conversation
    2. Keep the summary in the same language as the original conversation
    3. Target approximately {target_tokens} tokens
    4. Output the summary directly without any explanations or meta-commentary
    5. Format the summary as context information that can be used to continue the conversation
    6. Use {locale} language
    7. Start the output with a clear indicator that this is a summary (e.g., "[Summary of previous conversation]" or equivalent in the target language)

    {additional_context}

    <conversation>
    {content}
    </conversation>
""".trimIndent()

/**
 * 「总结窗口」默认提示词。可用变量：{content}、{locale}。
 *
 * 这里只给一个最小可用的默认值，方便直接试跑；设置页里可以整段替换成自己的提示词。
 * 若提示词中不含 {content}，会话内容会自动追加到末尾，所以留空也不会把对话丢掉。
 */
internal val DEFAULT_SUMMARY_PROMPT = """
    Summarize the following conversation window into a single long-term memory entry.

    Requirements:
    1. Keep only durable, reusable facts about the user and the decisions reached
    2. Drop small talk, pleasantries, and anything already obvious from context
    3. Write it as a compact third-person note, not a dialogue transcript
    4. Output the note directly, with no preamble or meta-commentary
    5. Use {locale} language

    <conversation>
    {content}
    </conversation>
""".trimIndent()
