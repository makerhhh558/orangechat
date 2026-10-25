package me.rerere.rikkahub.data.ai

import me.rerere.ai.provider.Model
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.model.Assistant
import kotlin.uuid.Uuid

data class ResolvedChatModel(
    val model: Model?,
    // 会话指定的模型已不存在，回退到了助手/全局默认
    val fellBack: Boolean,
)

/**
 * 取模型优先级：会话 modelId > 助手默认模型 > 全局默认模型。
 */
fun resolveConversationModel(
    settings: Settings,
    conversationModelId: Uuid?,
    assistant: Assistant?,
): ResolvedChatModel {
    if (conversationModelId != null) {
        val model = settings.findModelById(conversationModelId)
        if (model != null) return ResolvedChatModel(model, fellBack = false)
    }
    return ResolvedChatModel(
        model = defaultChatModel(settings, assistant),
        fellBack = conversationModelId != null,
    )
}

fun defaultChatModel(settings: Settings, assistant: Assistant?): Model? {
    assistant?.chatModelId?.let { id ->
        settings.findModelById(id)?.let { return it }
    }
    return settings.findModelById(settings.chatModelId)
}

fun defaultChatModelId(settings: Settings, assistant: Assistant?): Uuid? =
    defaultChatModel(settings, assistant)?.id
