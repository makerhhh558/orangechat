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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import coil3.compose.AsyncImage
import me.rerere.rikkahub.data.theme.AvatarStyle
import me.rerere.rikkahub.data.theme.ChatThemeRepository
import me.rerere.rikkahub.data.theme.rememberThemeImageModel
import me.rerere.rikkahub.data.theme.toShapeOrNull
import org.koin.compose.koinInject
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

/**
 * 头像挂件。
 *
 * css 优先：写了 [AvatarStyle.frameSource] 就用主题图片加载（和贴纸同一条链路），
 * 加载失败不显示；写了 `none`（[AvatarStyle.frameDisabled]）则连「显示设置」里的挂件也关掉。
 * 两者都没写时回退显示设置里的本地挂件。
 *
 * 外层 Box 已固定成头像尺寸。这里用 [wrapContentSize] 放开测量，
 * 挂件可以比头像大、居中溢出，但不会撑大占位、也不会挤动气泡和昵称。
 */
@Composable
private fun AvatarFrameOverlay(
    style: AvatarStyle,
    fallbackPath: String,
    fallbackOffsetX: Float,
    fallbackOffsetY: Float,
    fallbackScale: Float,
) {
    if (style.frameDisabled) return
    val source = style.frameSource?.trim()?.takeIf { it.isNotEmpty() }
    if (source != null) {
        val spec = LocalChatThemeSpec.current
        val model = rememberThemeImageModel(source, spec.baseDir) ?: return
        val themeRepo: ChatThemeRepository = koinInject()
        val themeName = spec.name
        val frameDp = style.frameSizeDp ?: style.size * (style.frameSizePercent ?: 1.4f)
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onError = {
                themeRepo.reportAvatarFrameLoadFailure(themeName, source, style.frameSourceLine)
            },
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .requiredSize(frameDp.dp)
                .offset(x = style.frameOffsetX.dp, y = style.frameOffsetY.dp),
        )
        return
    }
    if (fallbackPath.isBlank() || !File(fallbackPath).exists()) return
    val bitmap = remember(fallbackPath) {
        runCatching { BitmapFactory.decodeFile(fallbackPath)?.asImageBitmap() }.getOrNull()
    } ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .requiredSize((style.size * fallbackScale).dp)
            .offset(x = fallbackOffsetX.dp, y = fallbackOffsetY.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun ChatMessageUserAvatar(
    message: UIMessage,
    avatar: Avatar,
    nickname: String,
    modifier: Modifier = Modifier,
    /** position: side 时头像挪到气泡旁边，昵称行不再画头像 */
    hideAvatar: Boolean = false,
    /** 只画头像，供气泡侧边对齐使用 */
    iconsOnly: Boolean = false,
) {
    val settings = LocalSettings.current
    val style = LocalChatThemeSpec.current.userAvatar
    val visible = style.visible ?: settings.displaySetting.showUserAvatar
    if (message.role == MessageRole.USER && !message.parts.isEmptyUIMessage() && visible) {
        Row(
            modifier = if (iconsOnly) modifier else modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(style.gap.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!iconsOnly) Column(
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
            if (!hideAvatar) Box(
                modifier = Modifier.size(style.size.dp),
                contentAlignment = Alignment.Center,
            ) {
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
                    style = style,
                    fallbackPath = settings.displaySetting.userAvatarFramePath,
                    fallbackOffsetX = settings.displaySetting.userAvatarFrameOffsetX,
                    fallbackOffsetY = settings.displaySetting.userAvatarFrameOffsetY,
                    fallbackScale = settings.displaySetting.userAvatarFrameScale,
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
    /** position: side 时头像挪到气泡旁边，昵称行不再画头像 */
    hideAvatar: Boolean = false,
    /** 只画头像，供气泡侧边对齐使用 */
    iconsOnly: Boolean = false,
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
                if (showIcon && !hideAvatar) {
                    Box(
                        modifier = Modifier.size(style.size.dp),
                        contentAlignment = Alignment.Center,
                    ) {
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
                            style = style,
                            fallbackPath = settings.displaySetting.aiAvatarFramePath,
                            fallbackOffsetX = settings.displaySetting.aiAvatarFrameOffsetX,
                            fallbackOffsetY = settings.displaySetting.aiAvatarFrameOffsetY,
                            fallbackScale = settings.displaySetting.aiAvatarFrameScale,
                        )
                    }
                }
                if (!iconsOnly) Column(
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
                if (showIcon && !hideAvatar) {
                    Box(
                        modifier = Modifier.size(style.size.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AutoAIIcon(
                            name = model.modelId,
                            modifier = Modifier
                                .size(style.size.dp)
                                .avatarBorder(style),
                            loading = loading,
                            shapeOverride = style.toShapeOrNull(),
                        )
                        AvatarFrameOverlay(
                            style = style,
                            fallbackPath = settings.displaySetting.aiAvatarFramePath,
                            fallbackOffsetX = settings.displaySetting.aiAvatarFrameOffsetX,
                            fallbackOffsetY = settings.displaySetting.aiAvatarFrameOffsetY,
                            fallbackScale = settings.displaySetting.aiAvatarFrameScale,
                        )
                    }
                }
                if (!iconsOnly) Column(
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