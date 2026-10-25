package me.rerere.rikkahub.data.ai

import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.Uuid

class ConversationModelResolverTest {
    private val modelX = Model(modelId = "x", displayName = "X")
    private val modelY = Model(modelId = "y", displayName = "Y")
    private val modelG = Model(modelId = "g", displayName = "G")

    private fun settings(models: List<Model>, globalId: Uuid = modelG.id) = Settings(
        chatModelId = globalId,
        providers = listOf(ProviderSetting.OpenAI(name = "test", models = models)),
    )

    @Test
    fun `pinned model wins over assistant and global defaults`() {
        // A 窗口固定 X；B 窗口切到 Y 不影响 A
        val s = settings(listOf(modelX, modelY, modelG))
        val assistant = Assistant(chatModelId = modelY.id)
        val a = resolveConversationModel(s, modelX.id, assistant)
        val b = resolveConversationModel(s, modelY.id, assistant)
        assertEquals(modelX.id, a.model?.id)
        assertEquals(modelY.id, b.model?.id)
        assertFalse(a.fellBack)
    }

    @Test
    fun `deleted pinned model falls back to assistant model`() {
        val s = settings(listOf(modelY, modelG))
        val r = resolveConversationModel(s, modelX.id, Assistant(chatModelId = modelY.id))
        assertEquals(modelY.id, r.model?.id)
        assertTrue(r.fellBack)
    }

    @Test
    fun `deleted pinned and assistant model fall back to global`() {
        val s = settings(listOf(modelG))
        val r = resolveConversationModel(s, modelX.id, Assistant(chatModelId = modelY.id))
        assertEquals(modelG.id, r.model?.id)
        assertTrue(r.fellBack)
    }

    @Test
    fun `assistant without model uses global`() {
        val s = settings(listOf(modelX, modelG))
        val r = resolveConversationModel(s, null, Assistant(chatModelId = null))
        assertEquals(modelG.id, r.model?.id)
        assertFalse(r.fellBack)
    }

    @Test
    fun `null conversation model uses default without fallback notice`() {
        val s = settings(listOf(modelX, modelY, modelG))
        val r = resolveConversationModel(s, null, Assistant(chatModelId = modelY.id))
        assertEquals(modelY.id, r.model?.id)
        assertFalse(r.fellBack)
        assertEquals(modelY.id, defaultChatModelId(s, Assistant(chatModelId = modelY.id)))
    }

    @Test
    fun `everything deleted returns null without crashing`() {
        val s = settings(emptyList())
        val r = resolveConversationModel(s, modelX.id, Assistant(chatModelId = modelY.id))
        assertNull(r.model)
        assertTrue(r.fellBack)
    }
}
