/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.theme.ChatThemeEntry
import me.rerere.rikkahub.data.theme.ChatThemeRepository
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingDisplayChatThemePage() {
    val vm: SettingVM = koinViewModel()
    val repo: ChatThemeRepository = koinInject()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val loadState by repo.loadState.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val displaySetting = settings.displaySetting
    var themes by remember { mutableStateOf<List<ChatThemeEntry>>(emptyList()) }
    var refreshToken by remember { mutableStateOf(0) }

    LaunchedEffect(refreshToken) {
        themes = repo.listThemes()
    }

    fun updateDisplaySetting(block: (DisplaySetting) -> DisplaySetting) {
        val updated = block(settings.displaySetting)
        vm.updateSettings(settings.copy(displaySetting = updated))
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val displayName = uri.lastPathSegment
            repo.importTheme(uri, displayName)
                .onSuccess { name ->
                    refreshToken++
                    toaster.show("已导入主题: $name")
                }
                .onFailure { e ->
                    toaster.show("导入失败: ${e.message ?: "未知错误"}")
                }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("聊天主题 (CSS)") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("总开关") },
                ) {
                    item(
                        headlineContent = { Text("启用 CSS 聊天主题") },
                        supportingContent = { Text("关闭后使用内置样式，不影响其他显示设置") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.chatThemeEnabled,
                                onCheckedChange = { checked ->
                                    updateDisplaySetting { it.copy(chatThemeEnabled = checked) }
                                },
                            )
                        },
                    )
                }
            }

            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("主题列表") },
                ) {
                    if (themes.isEmpty()) {
                        item(
                            headlineContent = { Text("还没有主题") },
                            supportingContent = { Text("点下面的\"生成示例主题\"，或用\"导入主题\"选一个 css / zip") },
                        )
                    }
                    themes.forEach { entry ->
                        val selected = displaySetting.chatThemeName == entry.name
                        item(
                            onClick = {
                                updateDisplaySetting {
                                    it.copy(chatThemeName = entry.name, chatThemeEnabled = true)
                                }
                            },
                            headlineContent = { Text(entry.name) },
                            supportingContent = {
                                Text(
                                    if (entry.cssFiles.isEmpty()) "没有 css 文件"
                                    else entry.cssFiles.joinToString { it.name }
                                )
                            },
                            leadingContent = {
                                RadioButton(selected = selected, onClick = null)
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (repo.deleteTheme(entry.name)) {
                                                if (selected) {
                                                    updateDisplaySetting { it.copy(chatThemeName = "") }
                                                }
                                                refreshToken++
                                                toaster.show("已删除 ${entry.name}")
                                            } else {
                                                toaster.show("删除失败")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(HugeIcons.Delete01, contentDescription = "删除")
                                }
                            },
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf("text/css", "application/zip", "application/octet-stream", "*/*")
                                )
                            }
                        ) { Text("导入主题") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repo.reload(
                                        if (displaySetting.chatThemeEnabled) displaySetting.chatThemeName else ""
                                    )
                                    refreshToken++
                                    toaster.show("已重新加载")
                                }
                            }
                        ) { Text("重新加载") }
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repo.createSampleTheme()
                                    .onSuccess { name ->
                                        refreshToken++
                                        toaster.show("已生成示例主题: $name")
                                    }
                                    .onFailure { e ->
                                        toaster.show("生成失败: ${e.message ?: "未知错误"}")
                                    }
                            }
                        }
                    ) { Text("生成示例主题") }
                }
            }

            if (loadState.errors.isNotEmpty()) {
                item {
                    CardGroup(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        title = { Text("解析错误") },
                    ) {
                        loadState.errors.forEach { issue ->
                            item(
                                headlineContent = {
                                    Text(
                                        text = issue.toString(),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (loadState.unknownProperties.isNotEmpty()) {
                item {
                    CardGroup(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        title = { Text("未生效的属性") },
                    ) {
                        loadState.unknownProperties.forEach { issue ->
                            item(
                                headlineContent = {
                                    Text(
                                        text = issue.toString(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("主题目录") },
                ) {
                    item(
                        headlineContent = {
                            Text(
                                text = repo.themesDir.absolutePath,
                                style = MaterialTheme.typography.bodySmall,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text("在该目录下新建文件夹，放入任意数量 .css 文件（同名属性后加载的覆盖先加载的）")
                        },
                    )
                }
            }
        }
    }
}
