/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.message
 
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageAnnotation
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.isEmptyUIMessage
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.MusicNote03
import me.rerere.hugeicons.stroke.PlayCircle
import me.rerere.hugeicons.stroke.PauseCircle
import me.rerere.hugeicons.stroke.Video01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.data.model.replaceRegexes
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.richtext.ZoomableAsyncImage
import me.rerere.rikkahub.ui.components.richtext.buildMarkdownPreviewHtml
import me.rerere.rikkahub.ui.components.ui.ChainOfThought
import me.rerere.rikkahub.ui.components.ui.Favicon
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.modifier.shimmer
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.context.ChatThemeTextOverrides
import me.rerere.rikkahub.ui.context.LocalChatThemeSpec
import me.rerere.rikkahub.ui.context.LocalChatThemeText
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.data.theme.AvatarAlign
import me.rerere.rikkahub.data.theme.AvatarPositionSpec
import me.rerere.rikkahub.data.theme.BubbleSticker
import me.rerere.rikkahub.data.theme.BubbleStyle
import me.rerere.rikkahub.data.theme.ChatThemeRepository
import me.rerere.rikkahub.data.theme.CssTextUnit
import me.rerere.rikkahub.data.theme.StickerCorner
import me.rerere.rikkahub.data.theme.TailSide
import me.rerere.rikkahub.data.theme.bubbleGradient
import me.rerere.rikkahub.data.theme.bubbleShadow
import me.rerere.rikkahub.data.theme.nineSlice
import me.rerere.rikkahub.data.theme.rememberBorderImageBitmap
import me.rerere.rikkahub.data.theme.rememberCssFontFamily
import me.rerere.rikkahub.data.theme.rememberThemeImageModel
import me.rerere.rikkahub.data.theme.side
import me.rerere.rikkahub.data.theme.tailSpace
import me.rerere.rikkahub.data.theme.toShape
import me.rerere.rikkahub.data.theme.withUniformRadius
import me.rerere.rikkahub.ui.theme.extendColors
import me.rerere.rikkahub.data.datastore.ChatFontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.Image
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.base64Encode
import me.rerere.rikkahub.utils.openUrl
import coil3.compose.AsyncImage
import org.koin.compose.koinInject
import me.rerere.rikkahub.utils.splitIntoBubbleSegments
import me.rerere.rikkahub.utils.urlDecode
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
 
@Composable
fun ChatMessage(
    node: MessageNode,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    model: Model? = null,
    assistant: Assistant? = null,
    lastMessage: Boolean = false,
    onFork: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (MessageNode) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onTranslate: ((UIMessage, Locale) -> Unit)? = null,
    onClearTranslation: (UIMessage) -> Unit = {},
    onToolApproval: ((toolCallId: String, approved: Boolean, reason: String) -> Unit)? = null,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)? = null,
) {
    val message = node.messages[node.selectIndex]
    val settings = LocalDisplaySettings.current
    val textStyle = LocalTextStyle.current.copy(
        fontSize = LocalTextStyle.current.fontSize * settings.fontSizeRatio,
        color = settings.chatTextColor?.let { it.toComposeColor() } ?: Color.Unspecified,
        lineHeight = LocalTextStyle.current.lineHeight * settings.fontSizeRatio,
        fontFamily = when (settings.chatFontFamily) {
            ChatFontFamily.DEFAULT -> FontFamily.Default
            ChatFontFamily.SERIF -> FontFamily.Serif
            ChatFontFamily.MONOSPACE -> FontFamily.Monospace
            ChatFontFamily.CUSTOM -> {
                val fontPath = settings.customFontPath
                if (fontPath.isNotBlank() && java.io.File(fontPath).exists()) {
                    FontFamily(Font(java.io.File(fontPath)))
                } else {
                    FontFamily.Default
                }
            }
        }
    )
    var showActionsSheet by remember { mutableStateOf(false) }
    var showSelectCopySheet by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val themeSpec = LocalChatThemeSpec.current
    val sideAvatarStyle = if (message.role == MessageRole.USER) themeSpec.userAvatar else themeSpec.assistantAvatar
    // 头像并排只在主题启用且 position: side 时生效；top 保持原来的昵称行
    val sideAvatar = settings.chatThemeEnabled &&
        sideAvatarStyle.position == AvatarPositionSpec.SIDE &&
        !message.parts.isEmptyUIMessage()
    val sideAlignment = when (sideAvatarStyle.align) {
        AvatarAlign.CENTER -> Alignment.CenterVertically
        AvatarAlign.BOTTOM -> Alignment.Bottom
        AvatarAlign.TOP -> Alignment.Top
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.role == MessageRole.USER) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!message.parts.isEmptyUIMessage()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                ChatMessageAssistantAvatar(
                    message = message,
                    model = model,
                    assistant = assistant,
                    loading = loading,
                    hideAvatar = sideAvatar,
                    modifier = Modifier.weight(1f)
                )
                ChatMessageUserAvatar(
                    message = message,
                    avatar = settings.userAvatar,
                    nickname = settings.userNickname,
                    hideAvatar = sideAvatar,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        ProvideTextStyle(textStyle) {
            if (sideAvatar) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = sideAlignment,
                    horizontalArrangement = Arrangement.spacedBy(
                        sideAvatarStyle.gap.dp,
                        if (message.role == MessageRole.USER) Alignment.End else Alignment.Start,
                    ),
                ) {
                    if (message.role != MessageRole.USER) {
                        ChatMessageAssistantAvatar(
                            message = message,
                            model = model,
                            assistant = assistant,
                            loading = loading,
                            iconsOnly = true,
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        MessagePartsBlock(
                            assistant = assistant,
                            role = message.role,
                            parts = message.parts,
                            annotations = message.annotations,
                            loading = loading,
                            model = model,
                            stickerSeed = message.id.hashCode(),
                            onToolApproval = onToolApproval,
                            onToolAnswer = onToolAnswer,
                            onUserMessageClick = if (message.role == MessageRole.USER) onEdit else null,
                        )
                        message.translation?.let { translation ->
                            CollapsibleTranslationText(
                                content = translation,
                                onClickCitation = {}
                            )
                        }
                    }
                    if (message.role == MessageRole.USER) {
                        ChatMessageUserAvatar(
                            message = message,
                            avatar = settings.userAvatar,
                            nickname = settings.userNickname,
                            iconsOnly = true,
                        )
                    }
                }
            } else {
            MessagePartsBlock(
                assistant = assistant,
                role = message.role,
                parts = message.parts,
                annotations = message.annotations,
                loading = loading,
                model = model,
                stickerSeed = message.id.hashCode(),
                onToolApproval = onToolApproval,
                onToolAnswer = onToolAnswer,
                onUserMessageClick = if (message.role == MessageRole.USER) onEdit else null,
            )

            message.translation?.let { translation ->
                CollapsibleTranslationText(
                    content = translation,
                    onClickCitation = {}
                )
            }
            }
        }
 
        val showActions = if (lastMessage) {
            !loading
        } else {
            message.parts.isEmptyUIMessage().not()
        }
 
        AnimatedVisibility(
            visible = showActions,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                ChatMessageActionButtons(
                    message = message,
                    onRegenerate = onRegenerate,
                    node = node,
                    onUpdate = onUpdate,
                    onOpenActionSheet = {
                        showActionsSheet = true
                    },
                    onTranslate = onTranslate,
                    onClearTranslation = onClearTranslation
                )
            }
        }
 
        ProvideTextStyle(textStyle) {
            ChatMessageNerdLine(message = message)
        }
    }
    if (showActionsSheet) {
        ChatMessageActionsSheet(
            message = message,
            onEdit = onEdit,
            onDelete = onDelete,
            onShare = onShare,
            onFork = onFork,
            model = model,
            onSelectAndCopy = {
                showSelectCopySheet = true
            },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onWebViewPreview = {
                val textContent = message.parts
                    .filterIsInstance<UIMessagePart.Text>()
                    .joinToString("\n\n") { it.text }
                    .trim()
                if (textContent.isNotBlank()) {
                    val htmlContent = buildMarkdownPreviewHtml(
                        context = context,
                        markdown = textContent,
                        colorScheme = colorScheme
                    )
                    navController.navigate(Screen.WebView(content = htmlContent.base64Encode()))
                }
            },
            onDismissRequest = {
                showActionsSheet = false
            }
        )
    }
 
    if (showSelectCopySheet) {
        ChatMessageCopySheet(
            message = message,
            onDismissRequest = {
                showSelectCopySheet = false
            }
        )
    }
}
 
@OptIn(FlowPreview::class)
@Composable
private fun MessagePartsBlock(
    assistant: Assistant?,
    role: MessageRole,
    model: Model?,
    parts: List<UIMessagePart>,
    annotations: List<UIMessageAnnotation>,
    loading: Boolean,
    /** 贴纸随机角度的种子，保证同一条消息角度固定、滚动时不跳 */
    stickerSeed: Int = 0,
    onToolApproval: ((toolCallId: String, approved: Boolean, reason: String) -> Unit)? = null,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)? = null,
    onUserMessageClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
 
    // 消息输出HapticFeedback
    val hapticFeedback = LocalHapticFeedback.current
    val displaySettings = LocalDisplaySettings.current
    val bubbleAlpha = 1f - displaySettings.chatBubbleTransparency / 100f
    val partsState by rememberUpdatedState(parts)

    // 未启用 CSS 主题时，沿用"显示设置"里的圆角滑块，保证既有行为不变
    val themeSpec = LocalChatThemeSpec.current
    val themeActive = displaySettings.chatThemeEnabled
    val userBubbleStyle = remember(themeSpec, themeActive, displaySettings.bubbleCornerRadius) {
        if (themeActive) themeSpec.userBubble
        else themeSpec.userBubble.withUniformRadius(displaySettings.bubbleCornerRadius)
    }
    val assistantBubbleStyle = remember(themeSpec, themeActive, displaySettings.bubbleCornerRadius) {
        if (themeActive) themeSpec.assistantBubble
        else themeSpec.assistantBubble.withUniformRadius(displaySettings.bubbleCornerRadius)
    }
 
    val handleClickCitation: (String) -> Unit = remember {
        handler@{ citationId ->
            partsState.forEach { part ->
                if (part is UIMessagePart.Tool && part.toolName == "search_web" && part.isExecuted) {
                    val outputText = part.output.filterIsInstance<UIMessagePart.Text>().joinToString("\n") { it.text }
                    val items =
                        runCatching { JsonInstant.parseToJsonElement(outputText).jsonObject["items"]?.jsonArray }.getOrNull()
                            ?: return@forEach
                    items.forEach { item ->
                        val id = item.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
                        val url = item.jsonObject["url"]?.jsonPrimitive?.content ?: return@forEach
                        if (citationId == id) {
                            context.openUrl(url)
                            return@handler
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(displaySettings) {
        snapshotFlow { partsState }
            .debounce(50.milliseconds)
            .collect { parts ->
                if (parts.isNotEmpty() && loading && displaySettings.enableMessageGenerationHapticEffect) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                }
            }
    }
 
    // Render parts in original order (group thinking/tool as chain-of-thought)
    val groupedParts = remember(parts) { parts.groupMessageParts() }
    groupedParts.fastForEach { block ->
        when (block) {
            is MessagePartBlock.ThinkingBlock -> {
                if (block.steps.isNotEmpty()) {
                    val isReasoningOnlyBlock = block.steps.fastAll { it is ThinkingStep.ReasoningStep }
                    ChainOfThought(
                        modifier = Modifier.animateContentSize(),
                        steps = block.steps,
                        collapsedAdaptiveWidth = isReasoningOnlyBlock,
                    ) { step ->
                        when (step) {
                            is ThinkingStep.ReasoningStep -> {
                                key(step.reasoning.createdAt) {
                                    ChatMessageReasoningStep(
                                        reasoning = step.reasoning,
                                        model = model,
                                        assistant = assistant,
                                        collapsedAdaptiveWidth = isReasoningOnlyBlock,
                                    )
                                }
                            }
 
                            is ThinkingStep.ToolStep -> {
                                key(step.tool.toolCallId.ifBlank { step.hashCode().toString() }) {
                                    ChatMessageToolStep(
                                        tool = step.tool,
                                        loading = loading && !step.tool.isExecuted,
                                        allParts = parts,
                                        onToolApproval = onToolApproval,
                                        onToolAnswer = onToolAnswer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
 
            is MessagePartBlock.ContentBlock -> key(block.index) {
                when (val part = block.part) {
                    is UIMessagePart.Text -> {
                        // 从显示文本中移除[zip:...]标记
                        val displayText = remember(part.text) {
                            part.text.replace(Regex("\\[zip:[^\\]]+\\]", RegexOption.IGNORE_CASE), "")
                        }
                        
                        // 字体属性在这里统一注入：下面六处 MarkdownBlock 都吃
                        // LocalTextStyle 的默认值，所以包一层就够，不用逐处传 style
                        BubbleTextScope(
                            style = if (role == MessageRole.USER) userBubbleStyle else assistantBubbleStyle
                        ) {
                        SelectionContainer {
                            Column {
                                if (role == MessageRole.USER) {
                                    if (assistant?.splitUserBubbleByLine == true) {
                                        // 分气泡: 按用户输入的换行 (\n) 拆成多个独立气泡,
                                        // 拆分逻辑见 splitIntoBubbleSegments (会保护代码块/表格内部的换行)
                                        val bubbleSegments = remember(displayText) {
                                            displayText.splitIntoBubbleSegments()
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(userBubbleStyle.segmentSpacing.dp),
                                            horizontalAlignment = Alignment.End,
                                        ) {
                                            bubbleSegments.fastForEachIndexed { segIndex, segment ->
                                                key(segIndex) {
                                                    BubbleSurface(
                                                        imagePath = displaySettings.userBubbleImagePath,
                                                        style = userBubbleStyle,
                                                        isUser = true,
                                                        color = userBubbleStyle.backgroundColor?.let { it.toComposeColor() }
                                                            ?: displaySettings.userBubbleColor?.let { it.toComposeColor() }
                                                            ?: MaterialTheme.colorScheme.secondaryContainer,
                                                        overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                                        bubbleAlpha = bubbleAlpha,
                                                        stickerSeed = stickerSeed,
                                                        onClick = { onUserMessageClick?.invoke() },
                                                    ) {
                                                        MarkdownBlock(
                                                            content = segment.replaceRegexes(
                                                                assistant = assistant,
                                                                scope = AssistantAffectScope.USER,
                                                                visual = true,
                                                            ),
                                                            onClickCitation = handleClickCitation
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        BubbleSurface(
                                            imagePath = displaySettings.userBubbleImagePath,
                                            style = userBubbleStyle,
                                            isUser = true,
                                            color = userBubbleStyle.backgroundColor?.let { it.toComposeColor() }
                                                ?: displaySettings.userBubbleColor?.let { it.toComposeColor() }
                                                ?: MaterialTheme.colorScheme.secondaryContainer,
                                            overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                            bubbleAlpha = bubbleAlpha,
                                            stickerSeed = stickerSeed,
                                            onClick = { onUserMessageClick?.invoke() },
                                        ) {
                                            MarkdownBlock(
                                                content = displayText.replaceRegexes(
                                                    assistant = assistant,
                                                    scope = AssistantAffectScope.USER,
                                                    visual = true,
                                                ),
                                                onClickCitation = handleClickCitation
                                            )
                                        }
                                    }
                                } else if (assistant?.splitBubbleByLine == true) {
                                    // 分气泡: 按模型自己写的换行 (\n) 拆成多个独立气泡,
                                    // 拆分逻辑见 splitIntoBubbleSegments (会保护代码块/表格内部的换行)
                                    val bubbleSegments = remember(displayText) {
                                        displayText.splitIntoBubbleSegments()
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(assistantBubbleStyle.segmentSpacing.dp),
                                    ) {
                                        bubbleSegments.fastForEachIndexed { segIndex, segment ->
                                            key(segIndex) {
                                                if (displaySettings.showAssistantBubble) {
                                                    BubbleSurface(
                                                        imagePath = displaySettings.assistantBubbleImagePath,
                                                        style = assistantBubbleStyle,
                                                        isUser = false,
                                                        color = assistantBubbleStyle.backgroundColor?.let { it.toComposeColor() }
                                                            ?: displaySettings.assistantBubbleColor?.let { it.toComposeColor() }
                                                            ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                                        bubbleAlpha = bubbleAlpha,
                                                        stickerSeed = stickerSeed,
                                                    ) {
                                                        MarkdownBlock(
                                                            content = segment.replaceRegexes(
                                                                assistant = assistant,
                                                                scope = AssistantAffectScope.ASSISTANT,
                                                                visual = true,
                                                            ),
                                                            onClickCitation = handleClickCitation,
                                                        )
                                                    }
                                                } else {
                                                    MarkdownBlock(
                                                        content = segment.replaceRegexes(
                                                            assistant = assistant,
                                                            scope = AssistantAffectScope.ASSISTANT,
                                                            visual = true,
                                                        ),
                                                        onClickCitation = handleClickCitation,
                                                        modifier = Modifier
                                                            .animateContentSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (displaySettings.showAssistantBubble) {
                                        BubbleSurface(
                                            imagePath = displaySettings.assistantBubbleImagePath,
                                            style = assistantBubbleStyle,
                                            isUser = false,
                                            color = assistantBubbleStyle.backgroundColor?.let { it.toComposeColor() }
                                                ?: displaySettings.assistantBubbleColor?.let { it.toComposeColor() }
                                                ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                            overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                            bubbleAlpha = bubbleAlpha,
                                            stickerSeed = stickerSeed,
                                        ) {
                                            MarkdownBlock(
                                                content = displayText.replaceRegexes(
                                                    assistant = assistant,
                                                    scope = AssistantAffectScope.ASSISTANT,
                                                    visual = true,
                                                ),
                                                onClickCitation = handleClickCitation,
                                            )
                                        }
                                    } else {
                                        MarkdownBlock(
                                            content = displayText.replaceRegexes(
                                                assistant = assistant,
                                                scope = AssistantAffectScope.ASSISTANT,
                                                visual = true,
                                            ),
                                            onClickCitation = handleClickCitation,
                                            modifier = Modifier
                                                .animateContentSize()
                                        )
                                    }
                                }
                                
                            }
                        }
                        }
                    }
 
                    is UIMessagePart.Video -> {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    part.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                Icon(HugeIcons.Video01, null)
                            }
                        }
                    }
 
                    is UIMessagePart.Audio -> {
                        AudioPlayerBubble(url = part.url)
                    }
 
                    is UIMessagePart.VoiceMessage -> {
                        VoiceMessageBubble(
                            voiceMessage = part,
                            isUser = role == MessageRole.USER,
                        )
                    }
 
                    is UIMessagePart.Image -> {
                        val isImageLoading =
                            part.url.isBlank() || part.url.matches(Regex("^data:image/[^;]*;base64,\\s*$"))
                        if (isImageLoading) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .shimmer(isLoading = true)
                            )
                        } else {
                            ZoomableAsyncImage(
                                model = part.url,
                                contentDescription = null,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .height(72.dp)
                            )
                        }
                    }
 
                    is UIMessagePart.Document -> {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    part.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (part.mime) {
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                                            Icon(
                                                painter = painterResource(R.drawable.docx),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
 
                                        "application/pdf" -> {
                                            Icon(
                                                painter = painterResource(R.drawable.pdf),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
 
                                        else -> {
                                            Icon(
                                                imageVector = HugeIcons.File02,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
 
                                    Text(
                                        text = part.fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 200.dp)
                                    )
                                }
                            }
                        }
                    }
 
                    else -> {
                        // Skip unknown part types (e.g., deprecated ToolCall, ToolResult, Search)
                    }
                }
            }
        }
    }
 
    // Annotations (always rendered at the end)
    if (annotations.isNotEmpty()) {
        Column(
            modifier = Modifier.animateContentSize(),
        ) {
            var expand by remember { mutableStateOf(false) }
            if (expand) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.extendColors.gray8.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = contentColor.copy(alpha = 0.2f),
                                    size = Size(width = 10f, height = size.height),
                                )
                            }
                            .padding(start = 16.dp)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        annotations.fastForEachIndexed { index, annotation ->
                            when (annotation) {
                                is UIMessageAnnotation.UrlCitation -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Favicon(annotation.url, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = buildAnnotatedString {
                                                append("${index + 1}. ")
                                                withLink(LinkAnnotation.Url(annotation.url)) {
                                                    append(annotation.title.urlDecode())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = {
                    expand = !expand
                }
            ) {
                Text(stringResource(R.string.citations_count, annotations.size))
            }
        }
    }
 
    // 工作区文件 chip: assistant 消息下方展示被 workspace_write_file/
    // workspace_edit_file 写入/编辑的文件, 点击可导出/分享。
    // 仅在归属工作区的 assistant 消息中渲染, 不影响用户消息和其它布局。
    if (role == MessageRole.ASSISTANT) {
        EditedFilesList(parts = parts, assistant = assistant)
    }
}
 
/**
 * 给气泡正文套上 css 的字体相关属性。
 *
 * 字号/字重/行高/字间距/字体族通过 [ProvideTextStyle] 往下继承；
 * 标题字号、代码块字号、链接色、行内代码底色在 Markdown 内部是硬编码的，
 * 走 [LocalChatThemeText] 这个额外的 CompositionLocal 传下去。
 *
 * 基准样式取 [LocalTextStyle]，也就是"显示设置"里的字号比例和字体已经算进去了，
 * css 只在它之上做覆盖。css 没写的项全部保持 null / Unspecified，行为与改动前一致。
 *
 * 只包住气泡正文，不包昵称和时间 —— 那两个在 MessagePartsBlock 之外。
 */
/** 气泡正文的默认基准字号（sp），对应 Material bodyLarge。只在 LocalTextStyle 没给出具体字号时用。 */
private const val DEFAULT_BODY_FONT_SIZE_SP = 16f

@Composable
private fun BubbleTextScope(
    style: BubbleStyle,
    content: @Composable () -> Unit,
) {
    val base = LocalTextStyle.current
    val density = LocalDensity.current
    val themeSpec = LocalChatThemeSpec.current
    val fontFamily = rememberCssFontFamily(style.fontFamily, themeSpec.baseDir)

    // 气泡这条 LocalTextStyle 链上没人定过具体字号（见 ChatMessage 里的 textStyle：
    // Unspecified * ratio 仍是 Unspecified），所以标题/代码块的缩放基准不能直接读 base.fontSize，
    // 否则倍数算不出来 —— 正文会变大但标题不动。取不到时退回 Material bodyLarge 的 16sp，
    // 并把"显示设置"里的字号比例乘进去，和正文的实际观感保持一致。
    val fontSizeRatio = LocalDisplaySettings.current.fontSizeRatio
    val baseSizeSp = if (base.fontSize.isSpecified && base.fontSize.value > 0f) {
        base.fontSize.value
    } else {
        DEFAULT_BODY_FONT_SIZE_SP * fontSizeRatio
    }

    val bodyStyle = remember(base, style, density, fontFamily, baseSizeSp) {
        // dp/px 写法要换成 sp 才能进 TextStyle，sp 写法直接用
        fun CssTextUnit.toTextUnit(): TextUnit =
            if (isSp) value.sp else with(density) { value.dp.toSp() }

        val cssFontSize = style.fontSize?.toTextUnit()
        // 倍数行高要有确定的字号可乘，base.fontSize 可能是 Unspecified
        val effectiveFontSize = cssFontSize
            ?: base.fontSize.takeIf { it.isSpecified }
            ?: baseSizeSp.sp
        val lineHeight = style.lineHeight?.let { lh ->
            if (lh.isMultiplier) {
                // 倍数行高要乘在"最终生效"的字号上
                if (effectiveFontSize.isSpecified) effectiveFontSize * lh.value
                else TextUnit.Unspecified
            } else {
                if (lh.isSp) lh.value.sp else with(density) { lh.value.dp.toSp() }
            }
        }
        base.copy(
            fontSize = cssFontSize ?: base.fontSize,
            fontWeight = style.fontWeight?.let { FontWeight(it) } ?: base.fontWeight,
            lineHeight = lineHeight ?: base.lineHeight,
            letterSpacing = style.letterSpacing?.toTextUnit() ?: base.letterSpacing,
            fontFamily = fontFamily ?: base.fontFamily,
        )
    }

    val overrides = remember(style, bodyStyle, baseSizeSp) {
        // 只有 css 显式写了 font-size 才按倍数带动标题和代码块，
        // 没写就保持它们原本的硬编码字号
        val scale = if (style.fontSize != null && bodyStyle.fontSize.isSpecified) {
            bodyStyle.fontSize.value / baseSizeSp
        } else null
        ChatThemeTextOverrides(
            fontScale = scale,
            linkColor = style.linkColor?.let { it.toComposeColor() },
            codeBackground = style.codeBackground?.let { it.toComposeColor() },
        )
    }

    CompositionLocalProvider(LocalChatThemeText provides overrides) {
        ProvideTextStyle(bodyStyle) { content() }
    }
}

/**
 * 气泡容器。
 *
 * 形状、内边距、描边、最大宽度都来自 CSS 主题 [style]；
 * [isUser] 用于让尖钩在 `tail-position: auto` 时自动贴到正确的一侧。
 *
 * 尖钩画在气泡本体之外，所以要在对应一侧额外留出 [tailSpace] 的内边距，
 * 否则内容会压到尖钩上。
 */
@Composable
private fun BubbleSurface(
    imagePath: String,
    style: BubbleStyle,
    isUser: Boolean,
    color: Color,
    overlayEnabled: Boolean,
    bubbleAlpha: Float,
    stickerSeed: Int = 0,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val themeSpec = LocalChatThemeSpec.current
    val themeRepo: ChatThemeRepository = koinInject()
    val skin = rememberBorderImageBitmap(
        borderImage = style.borderImage,
        baseDir = themeSpec.baseDir,
        onFailure = {
            val source = style.borderImage.source ?: return@rememberBorderImageBitmap
            themeRepo.reportBorderImageLoadFailure(themeSpec.name, source, style.borderImage.sourceLine)
        },
    )
    // 皮肤加载成功时不画背景、边框、阴影，尖钩强制关闭
    val drawStyle = if (skin != null) style.copy(tailVisible = false) else style
    val shape = remember(drawStyle, isUser) { drawStyle.toShape(isUser) }
    val tailSide = remember(drawStyle, isUser) {
        if (drawStyle.tailSpace > 0.dp) drawStyle.tailPosition.side(isUser) else null
    }
    val contentPadding = PaddingValues(
        start = drawStyle.paddingHorizontal.dp + if (tailSide == TailSide.LEFT) drawStyle.tailSpace else 0.dp,
        end = drawStyle.paddingHorizontal.dp + if (tailSide == TailSide.RIGHT) drawStyle.tailSpace else 0.dp,
        top = drawStyle.paddingVertical.dp,
        bottom = drawStyle.paddingVertical.dp,
    )
    val borderColor = style.borderColor?.let { it.toComposeColor() }
    val widthModifier = style.maxWidthPercent
        ?.let { Modifier.maxWidthFraction(it.coerceIn(0.1f, 1f)) }
        ?: Modifier
    val backgroundAlpha = (bubbleAlpha * style.backgroundOpacity).coerceIn(0f, 1f)
    val gradient = style.backgroundGradient?.takeIf { skin == null }

    val hasImage = skin == null && imagePath.isNotBlank() && java.io.File(imagePath).exists()
    // 贴纸要能露在气泡外，所以外面套一层不带 clip 的 Box。
    // 贴纸参与测量但用 offset 外伸，详见 BubbleStickerLayer
    BubbleStickerBox(sticker = style.sticker, seed = stickerSeed) {
    if (hasImage) {
        Box(
            modifier = Modifier
                // 阴影必须排在 animateContentSize 之前：后者内部带 clipToBounds，
                // 排在它后面的阴影会被裁掉
                .bubbleShadow(style.shadow, shape)
                .animateContentSize()
                .then(widthModifier)
                .clip(shape)
                .then(
                    if (style.borderWidth > 0f && borderColor != null) {
                        Modifier.border(style.borderWidth.dp, borderColor, shape)
                    } else Modifier
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (overlayEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color.copy(alpha = backgroundAlpha))
                )
            }
            Column(modifier = Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Surface(
            modifier = Modifier
                .then(if (skin == null) Modifier.bubbleShadow(style.shadow, shape) else Modifier)
                .then(
                    if (gradient != null) Modifier.bubbleGradient(gradient, shape, backgroundAlpha) else Modifier
                )
                .animateContentSize()
                .then(widthModifier),
            shape = shape,
            color = if (gradient != null || skin != null) Color.Transparent else color.copy(alpha = backgroundAlpha),
            border = if (skin == null && style.borderWidth > 0f && borderColor != null) {
                BorderStroke(style.borderWidth.dp, borderColor)
            } else null,
            onClick = onClick ?: {},
        ) {
            Box {
                if (skin != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .nineSlice(skin, style.borderImage.slice, style.borderImage.width)
                    )
                }
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
    }
    }
}

/**
 * 气泡贴纸的外层容器。
 *
 * 没配贴纸时直接画 [content]，不额外插入任何布局节点，保证既有行为不变。
 *
 * 配了贴纸时套一层 Box：
 * - Box 自身不加 clip，贴纸露在气泡外的部分不会被裁
 * - 贴纸挂在 [BoxScope.align] 上，用 `offset` 做偏移，负偏移往气泡外伸
 * - 贴纸层不接收触摸（没有 clickable / pointerInput），长按复制、点击重发都不受影响
 */
@Composable
private fun BubbleStickerBox(
    sticker: BubbleSticker,
    seed: Int,
    content: @Composable () -> Unit,
) {
    val themeSpec = LocalChatThemeSpec.current
    val model = rememberThemeImageModel(sticker.source, themeSpec.baseDir)
    if (model == null) {
        content()
        return
    }
    Box {
        content()
        BubbleStickerLayer(sticker = sticker, model = model, seed = seed)
    }
}

/** 把 [StickerCorner] 映射到 Box 的对齐方式。 */
private fun StickerCorner.toAlignment(): Alignment = when (this) {
    StickerCorner.TOP_LEFT -> Alignment.TopStart
    StickerCorner.TOP_RIGHT -> Alignment.TopEnd
    StickerCorner.BOTTOM_LEFT -> Alignment.BottomStart
    StickerCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
}

/**
 * 贴纸本体。
 *
 * 贴纸按 [BubbleSticker.corner] 用 `align` 嵌到气泡对应的角里，再用 `offset` 微调位置。
 * `offset` 只在放置阶段生效、不参与测量，所以负偏移能让贴纸正常露在气泡外面。
 *
 * 这里刻意不用「把测量尺寸报成 0」的写法：零尺寸节点被放到负坐标后不会被绘制，
 * 贴纸会完全看不见（图片其实已经加载成功）。代价是贴纸会参与父 Box 的尺寸计算，
 * 气泡比贴纸小的时候会被撑到贴纸那么大，正常字号下气泡都比贴纸大，影响可忽略。
 */
@Composable
private fun BoxScope.BubbleStickerLayer(
    sticker: BubbleSticker,
    model: Any,
    seed: Int,
) {
    // 随机角度用消息 id 当种子：同一条消息每次重组都是同一个角度，滚动时不会跳
    val angle = remember(seed, sticker.rotate, sticker.rotateRandom) {
        if (sticker.rotateRandom <= 0f) sticker.rotate
        else sticker.rotate + (Random(seed).nextFloat() * 2f - 1f) * sticker.rotateRandom
    }
    val sizeDp = sticker.size.dp
    val corner = sticker.corner
    val isRight = corner == StickerCorner.TOP_RIGHT || corner == StickerCorner.BOTTOM_RIGHT
    val isBottom = corner == StickerCorner.BOTTOM_LEFT || corner == StickerCorner.BOTTOM_RIGHT
    // 加载失败时把原因报回仓库，让主题设置页能看到；本地文件缺失在加载阶段已经报过
    val themeRepo: ChatThemeRepository = koinInject()
    val themeName = LocalChatThemeSpec.current.name
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onError = {
            val source = sticker.source
            if (!source.isNullOrBlank()) {
                themeRepo.reportStickerLoadFailure(themeName, source, sticker.sourceLine)
            }
        },
        modifier = Modifier
            // align 先把贴纸整体嵌进气泡对应的角里
            .align(corner.toAlignment())
            .size(sizeDp)
            .offset {
                // 偏移按"从角往气泡内部"算，所以右侧角的 x、底部角的 y 要取反。
                // 这样四个角都是统一的：正值往气泡里缩，负值往气泡外伸。
                IntOffset(
                    x = sticker.offsetX.dp.roundToPx().let { if (isRight) -it else it },
                    y = sticker.offsetY.dp.roundToPx().let { if (isBottom) -it else it },
                )
            }
            .graphicsLayer {
                rotationZ = angle
                alpha = sticker.opacity.coerceIn(0f, 1f)
            },
    )
}

/**
 * 把宽度上限收到父容器的 [fraction] 倍，但不强制占满。
 *
 * 不能用 `fillMaxWidth(fraction)`：那个会把宽度钉死在该比例上（既是上限也是下限），
 * 短消息也会被撑满。这里只改约束的 maxWidth，minWidth 保持 0，
 * 让内容自己决定实际宽度，超出上限才换行。
 */
private fun Modifier.maxWidthFraction(fraction: Float): Modifier = layout { measurable, constraints ->
    val limit = if (constraints.hasBoundedWidth) {
        (constraints.maxWidth * fraction).toInt().coerceAtLeast(1)
    } else {
        constraints.maxWidth
    }
    val placeable = measurable.measure(
        constraints.copy(minWidth = 0, maxWidth = limit)
    )
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}
 
@Composable
@Suppress("UnusedCrossTarget")
internal fun AudioPlayerBubble(url: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var currentMs by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
 
    // Generate pseudo-random waveform bar heights (deterministic per url)
    val waveformBars = remember(url) {
        val rnd = java.util.Random(url.hashCode().toLong())
        List(40) { 0.15f + rnd.nextFloat() * 0.85f }
    }
 
    val progress = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f
 
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
 
    // Progress ticker
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentMs = it.currentPosition
                }
            }
            kotlinx.coroutines.delay(50)
        }
    }
 
    // Animate waveform bars when playing
    val animatedBars = remember { mutableStateOf(waveformBars) }
    LaunchedEffect(isPlaying, progress) {
        if (isPlaying) {
            val rnd = java.util.Random()
            val newBars = waveformBars.mapIndexed { index, base ->
                val playedRatio = if (progress > 0f) index.toFloat() / waveformBars.size else 0f
                if (playedRatio <= progress) {
                    // Already played bars stay at original height
                    base
                } else {
                    // Upcoming bars get slight animation
                    base * (0.85f + rnd.nextFloat() * 0.3f)
                }
            }
            animatedBars.value = newBars
        } else {
            animatedBars.value = waveformBars
        }
    }
 
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
 
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play / Pause button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null || !isPrepared) {
                            val mp = MediaPlayer()
                            try {
                                val uri = android.net.Uri.parse(url)
                                mp.setDataSource(context, uri)
                                mp.prepare()
                                durationMs = mp.duration
                                mp.setOnCompletionListener {
                                    isPlaying = false
                                    currentMs = 0
                                }
                                mp.start()
                                isPlaying = true
                                isPrepared = true
                                mediaPlayer = mp
                            } catch (e: Exception) {
                                mp.release()
                            }
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) HugeIcons.PauseCircle else HugeIcons.PlayCircle,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
 
        Spacer(modifier = Modifier.width(8.dp))
 
        // Waveform bars
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .clickable { /* click waveform to seek (optional future) */ }
        ) {
            val barCount = animatedBars.value.size
            val totalWidth = size.width
            val barWidth = 2.5f
            val gap = (totalWidth - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
            val playedBarCount = (progress * barCount).toInt()
 
            animatedBars.value.forEachIndexed { index, barRatio ->
                val barHeight = size.height * barRatio.coerceIn(0.15f, 1f)
                val x = index * (barWidth + gap)
                val y = (size.height - barHeight) / 2f
                drawRoundRect(
                    color = if (index < playedBarCount) activeColor else inactiveColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
                )
            }
        }
 
        Spacer(modifier = Modifier.width(6.dp))
 
        // Duration text
        val displaySec = if (isPlaying || currentMs > 0) {
            val remaining = (durationMs - currentMs) / 1000
            remaining.coerceAtLeast(0)
        } else {
            durationMs / 1000
        }
        Text(
            text = String.format("%d:%02d", displaySec / 60, displaySec % 60),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 13.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
 
@Composable
internal fun VoiceMessageBubble(
    voiceMessage: UIMessagePart.VoiceMessage,
    isUser: Boolean,
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
 
    val durationSec = (voiceMessage.duration / 1000).coerceAtLeast(1)
 
    DisposableEffect(voiceMessage.url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
 
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    isPlaying = false
                }
            }
            kotlinx.coroutines.delay(50)
        }
    }
 
    val themeSpec = LocalChatThemeSpec.current
    val voiceStyle = if (isUser) themeSpec.userVoice else themeSpec.assistantVoice
    val themeRepo: ChatThemeRepository = koinInject()
    val skin = rememberBorderImageBitmap(
        borderImage = voiceStyle.borderImage,
        baseDir = themeSpec.baseDir,
        onFailure = {
            val source = voiceStyle.borderImage.source ?: return@rememberBorderImageBitmap
            themeRepo.reportBorderImageLoadFailure(themeSpec.name, source, voiceStyle.borderImage.sourceLine)
        },
    )
    val shape = voiceStyle.cornerRadiusPercent?.let { RoundedCornerShape(percent = it.toInt()) }
        ?: RoundedCornerShape(voiceStyle.cornerRadius.dp)
    val backgroundAlpha = voiceStyle.backgroundOpacity.coerceIn(0f, 1f)
    val gradient = voiceStyle.backgroundGradient?.takeIf { skin == null }
    val baseColor = voiceStyle.backgroundColor?.let { it.toComposeColor() }
        ?: if (isUser) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer
    val borderColor = voiceStyle.borderColor?.let { it.toComposeColor() }
    val textColor = voiceStyle.textColor?.let { it.toComposeColor() }
    BubbleStickerBox(sticker = voiceStyle.sticker, seed = voiceMessage.url.hashCode()) {
    Surface(
        modifier = Modifier
            .then(if (skin == null) Modifier.bubbleShadow(voiceStyle.shadow, shape) else Modifier)
            .then(if (gradient != null) Modifier.bubbleGradient(gradient, shape, backgroundAlpha) else Modifier),
        shape = shape,
        color = if (gradient != null || skin != null) Color.Transparent else baseColor.copy(alpha = backgroundAlpha),
        border = if (skin == null && voiceStyle.borderWidth > 0f && borderColor != null) {
            BorderStroke(voiceStyle.borderWidth.dp, borderColor)
        } else null,
        onClick = {
            if (isPlaying) {
                mediaPlayer?.let {
                    it.stop()
                    it.reset()
                }
                isPlaying = false
            } else {
                try {
                    val mp = MediaPlayer()
                    mp.setDataSource(voiceMessage.url)
                    mp.prepare()
                    mp.setOnCompletionListener {
                        isPlaying = false
                    }
                    mp.start()
                    isPlaying = true
                    mediaPlayer?.release()
                    mediaPlayer = mp
                } catch (e: Exception) {
                    // File might not exist
                }
            }
        },
    ) {
        Box {
            if (skin != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .nineSlice(skin, voiceStyle.borderImage.slice, voiceStyle.borderImage.width)
                )
            }
        Column(
            modifier = Modifier.padding(
                horizontal = voiceStyle.paddingHorizontal.dp,
                vertical = voiceStyle.paddingVertical.dp,
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) HugeIcons.PauseCircle else HugeIcons.PlayCircle,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(voiceStyle.iconSize.dp)
                )
                // Waveform bars
                val waveformBars = remember(voiceMessage.url, voiceStyle.barCount) {
                    val rnd = java.util.Random(voiceMessage.url.hashCode().toLong())
                    List(voiceStyle.barCount.coerceIn(1, 200)) { 0.2f + rnd.nextFloat() * 0.8f }
                }
                val waveformColor = voiceStyle.waveformColor?.let { it.toComposeColor() }
                    ?: if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                Canvas(
                    modifier = Modifier
                        .width(voiceStyle.waveformWidth.dp)
                        .height(voiceStyle.waveformHeight.dp)
                ) {
                    val barCount = waveformBars.size
                    val barWidth = voiceStyle.barWidth.coerceAtLeast(0.5f)
                    val gap = (size.width - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
                    waveformBars.forEachIndexed { index, barRatio ->
                        val barHeight = size.height * barRatio.coerceIn(0.2f, 1f)
                        val x = index * (barWidth + gap)
                        val y = (size.height - barHeight) / 2f
                        drawRoundRect(
                            color = waveformColor,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
                        )
                    }
                }
                Text(
                    text = "${durationSec}″",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor ?: if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            // Show transcript text below the voice bubble (like WeChat)
            if (voiceMessage.transcript.isNotBlank()) {
                Text(
                    text = voiceMessage.transcript,
                    style = MaterialTheme.typography.bodySmall,
                    color = (textColor ?: if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        }
    }
    }
}
 