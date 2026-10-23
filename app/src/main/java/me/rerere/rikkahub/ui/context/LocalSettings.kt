/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.context

import androidx.compose.runtime.compositionLocalOf
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.ai.mcp.McpServerConfig
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.QuickMessage
import me.rerere.rikkahub.data.theme.ChatThemeSpec

val LocalSettings = compositionLocalOf<Settings> {
    error("No SettingsStore provided")
}

val LocalDisplaySettings = compositionLocalOf<DisplaySetting> {
    error("LocalDisplaySettings not provided")
}

val LocalProviders = compositionLocalOf<List<ProviderSetting>> {
    error("LocalProviders not provided")
}

val LocalMcpServers = compositionLocalOf<List<McpServerConfig>> {
    error("LocalMcpServers not provided")
}

val LocalQuickMessages = compositionLocalOf<List<QuickMessage>> {
    error("LocalQuickMessages not provided")
}

val LocalCurrentAssistant = compositionLocalOf<Assistant> {
    error("LocalCurrentAssistant not provided")
}

val LocalCurrentChatModel = compositionLocalOf<Model?> {
    error("LocalCurrentChatModel not provided")
}

/**
 * 当前生效的聊天主题样式。
 * 没有提供时退回内置默认样式，保证任何预览/独立 Composable 都能正常渲染。
 */
val LocalChatThemeSpec = compositionLocalOf { ChatThemeSpec.BUILTIN }

/**
 * 气泡正文里 Markdown 需要的主题覆盖项。
 *
 * 字号、字重、行高这些能靠 [androidx.compose.ui.text.TextStyle] 继承传下去，
 * 但标题字号、代码块字号、链接色、行内代码底色在 Markdown 内部是硬编码的，
 * 没法通过 TextStyle 传递，只能另开一个 CompositionLocal。
 *
 * 默认值全为 null，即"CSS 没设置"——此时 Markdown 完全保持原有硬编码行为，
 * 不启用 CSS 主题的场景一行都不受影响。
 */
val LocalChatThemeText = compositionLocalOf { ChatThemeTextOverrides.NONE }

/**
 * @param fontScale 标题/代码块字号的缩放倍数，null = CSS 未设置 font-size，保持硬编码字号
 * @param linkColor 正文链接颜色，null = 用 primary
 * @param codeBackground 行内代码与代码块底色，null = 用主题默认
 */
data class ChatThemeTextOverrides(
    val fontScale: Float? = null,
    val linkColor: androidx.compose.ui.graphics.Color? = null,
    val codeBackground: androidx.compose.ui.graphics.Color? = null,
) {
    companion object {
        val NONE = ChatThemeTextOverrides()
    }
}
