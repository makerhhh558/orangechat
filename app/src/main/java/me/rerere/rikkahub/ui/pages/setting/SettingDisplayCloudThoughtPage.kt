/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_CLOUD_PROMPT
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.message.CloudPopupContent
import me.rerere.rikkahub.ui.components.message.CloudThoughtIcon
import me.rerere.rikkahub.ui.components.message.currentCloudTheme
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayCloudThoughtPage() {
    val vm: SettingVM = koinViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val displaySetting = settings.displaySetting

    fun updateDisplaySetting(setting: DisplaySetting) {
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("☁️心里话") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { contentPadding ->
        LazyColumn(
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("开关") },
                ) {
                    item(
                        headlineContent = { Text("☁️心里话") },
                        supportingContent = { Text("把提示词追加到系统提示词末尾，并在回复里显示☁️") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.cloudThoughtEnabled,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(cloudThoughtEnabled = it))
                                },
                            )
                        },
                    )
                    item(
                        headlineContent = { Text("隐藏思考链") },
                        supportingContent = { Text("只在聊天界面隐藏，不影响模型思考") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.hideReasoning,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(hideReasoning = it))
                                },
                            )
                        },
                    )
                }
            }
            item {
                CloudPromptEditor(
                    prompt = displaySetting.cloudThoughtPrompt,
                    onChange = { updateDisplaySetting(displaySetting.copy(cloudThoughtPrompt = it)) },
                )
            }
            item {
                CloudThoughtPreview()
            }
        }
    }
}

@Composable
private fun CloudPromptEditor(prompt: String, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(prompt) }
    CardGroup(
        modifier = Modifier.padding(horizontal = 8.dp),
        title = { Text("提示词") },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                )
                TextButton(
                    onClick = {
                        text = DEFAULT_CLOUD_PROMPT
                        onChange(DEFAULT_CLOUD_PROMPT)
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("恢复默认")
                }
            }
        }
    }
}

/** 预览直接平铺弹窗内容，不弹真正的 Popup，避免挡住设置页滚动。 */
@Composable
private fun CloudThoughtPreview() {
    val spec = currentCloudTheme()
    CardGroup(
        modifier = Modifier.padding(horizontal = 8.dp),
        title = { Text("效果预览") },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = "今天也想你了",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    CloudThoughtIcon(spec = spec)
                }
                CloudPopupContent(
                    text = "今天她先来找我了\n<mood>小得意</mood>\n<bar label=\"好奇\" value=\"30\"/>\n<bar label=\"开心\" value=\"60\"/>\n<bar label=\"想她\" value=\"92\"/>",
                    spec = spec,
                )
            }
        }
    }
}
