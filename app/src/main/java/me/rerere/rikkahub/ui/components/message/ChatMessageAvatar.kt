/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.message

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.toJavaLocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.isEmptyUIMessage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.theme.AvatarStyle
import me.rerere.rikkahub.data.theme.toShapeOrNull
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.context.LocalChatThemeSpec
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.utils.toLocalString
import java.io.File

/** 头像描边。宽度为 0 或没给颜色时什么都不做。 */
@Composable
private fun Modifier.avatarBorder(style: AvatarStyle): Modifier {
    if (style.borderWidth <= 0f) return this
    val color = style.borderColor?.toComposeColor() ?: LocalContentColor.current
    return this.border(
        border = BorderStroke(style.borderWidth.dp, color),
        shape = style.toShapeOrNull() ?: CircleShape,
    )
}

@Composable
private fun AvatarFrameOverlay(
    framePath: String,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    baseSize: Float,
) {
    if (framePath.isNotBlank() && File(framePath).exists()) {
        val context = LocalContext.current
        val bitmap = remember(framePath) {
            runCatching {
                BitmapFactory.decodeFile(framePath)?.asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Avatar Frame",
                modifier = Modifier
                    .size((baseSize * scale).dp)
                    .offset(x = offsetX.dp, y = offsetY.dp),
                contentScale = ContentScale.Fit,
                alpha = 1f,
            )
        }
    }
}

@Composable
fun ChatMessageUserAvatar(
    message: UIMessage,
    avatar: Avatar,
    nickname: String,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    val style = LocalChatThemeSpec.current.userAvatar
    val visible = style.visible ?: settings.displaySetting.showUserAvatar
    if (message.role == MessageRole.USER && !message.parts.isEmptyUIMessage() && visible) {
        Row(
            modifier = modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(style.gap.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = nickname.ifEmpty { stringResource(R.string.user_default_name) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    color = LocalContentColor.current.copy(alpha = 0.85f),
                )
                if (settings.displaySetting.showDateBelowName) {
                    Text(
                        text = message.createdAt.toJavaLocalDateTime().toLocalString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
            }
            Box(contentAlignment = Alignment.Center) {
                UIAvatar(
                    name = nickname,
                    modifier = Modifier
                        .size(style.size.dp)
                        .avatarBorder(style),
                    value = avatar,
                    loading = false,
                    shapeOverride = style.toShapeOrNull(),
                )
                AvatarFrameOverlay(
                    framePath = settings.displaySetting.userAvatarFramePath,
                    offsetX = settings.displaySetting.userAvatarFrameOffsetX,
                    offsetY = settings.displaySetting.userAvatarFrameOffsetY,
                    scale = settings.displaySetting.userAvatarFrameScale,
                    baseSize = style.size,
                )
            }
        }
    }
}

@Composable
fun ChatMessageAssistantAvatar(
    message: UIMessage,
    loading: Boolean,
    model: Model?,
    assistant: Assistant?,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    val style = LocalChatThemeSpec.current.assistantAvatar
    val showIcon = style.visible ?: settings.displaySetting.showModelIcon
    val useAssistantAvatar = assistant?.useAssistantAvatar == true
    if (message.role == MessageRole.ASSISTANT && (model != null || useAssistantAvatar)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(style.gap.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            if (useAssistantAvatar) {
                if (showIcon) {
                    Box(contentAlignment = Alignment.Center) {
                        UIAvatar(
                            name = assistant.name,
                            modifier = Modifier
                                .size(style.size.dp)
                                .avatarBorder(style),
                            value = assistant.avatar,
                            loading = loading,
                            shapeOverride = style.toShapeOrNull(),
                        )
                        AvatarFrameOverlay(
                            framePath = settings.displaySetting.aiAvatarFramePath,
                            offsetX = settings.displaySetting.aiAvatarFrameOffsetX,
                            offsetY = settings.displaySetting.aiAvatarFrameOffsetY,
                            scale = settings.displaySetting.aiAvatarFrameScale,
                            baseSize = style.size,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if(settings.displaySetting.showModelName) {
                        Text(
                            text = assistant.name.ifEmpty { stringResource(R.string.assistant_page_default_assistant) },
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            maxLines = 1,
                        )
                        if (settings.displaySetting.showDateBelowName) {
                            Text(
                                text = message.createdAt.toJavaLocalDateTime().toLocalString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = LocalContentColor.current.copy(alpha = 0.8f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            } else if (model != null) {
                if (showIcon) {
                    Box(contentAlignment = Alignment.Center) {
                        AutoAIIcon(
                            name = model.modelId,
                            modifier = Modifier
                                .size(style.size.dp)
                                .avatarBorder(style),
                            loading = loading,
                            shapeOverride = style.toShapeOrNull(),
                        )
                        AvatarFrameOverlay(
                            framePath = settings.displaySetting.aiAvatarFramePath,
                            offsetX = settings.displaySetting.aiAvatarFrameOffsetX,
                            offsetY = settings.displaySetting.aiAvatarFrameOffsetY,
                            scale = settings.displaySetting.aiAvatarFrameScale,
                            baseSize = style.size,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if(settings.displaySetting.showModelName) {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleSmallEmphasized,
                        )
                        if (settings.displaySetting.showDateBelowName) {
                            Text(
                                text = message.createdAt.toJavaLocalDateTime().toLocalString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalContentColor.current.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}