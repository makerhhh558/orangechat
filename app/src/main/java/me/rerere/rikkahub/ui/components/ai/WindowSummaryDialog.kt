/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.ui.RabbitLoadingIndicator

/**
 * 「总结窗口」的预览对话框。
 *
 * 打开即开始生成，生成中可取消；拿到结果后内容可编辑，可保存进记忆或重新生成。
 * 生成失败时（错误提示由 ViewModel 统一弹出）直接关闭，不会停在空白框里。
 */
@Composable
fun WindowSummaryDialog(
    onDismiss: () -> Unit,
    onGenerate: (onResult: (String) -> Unit) -> Job,
    onSave: (content: String, onSaved: () -> Unit) -> Job,
) {
    var summary by remember { mutableStateOf("") }
    var hasResult by remember { mutableStateOf(false) }
    var generateJob by remember { mutableStateOf<Job?>(null) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    val isGenerating = generateJob?.isActive == true
    val isSaving = saveJob?.isActive == true
    val busy = isGenerating || isSaving

    fun startGenerate() {
        summary = ""
        hasResult = false
        generateJob = onGenerate { result ->
            summary = result
            hasResult = true
        }
    }

    LaunchedEffect(Unit) {
        startGenerate()
    }

    LaunchedEffect(generateJob) {
        val job = generateJob ?: return@LaunchedEffect
        job.join()
        // 失败或被取消时没有结果，关掉对话框
        if (!hasResult) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        title = {
            Text(stringResource(R.string.chat_page_summarize_window_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RabbitLoadingIndicator(
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.chat_page_summarize_window_generating))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.chat_page_summarize_window_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = {
                            Text(stringResource(R.string.chat_page_summarize_window_result_label))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 10,
                        enabled = !isSaving,
                    )
                }
            }
        },
        confirmButton = {
            if (isGenerating) {
                TextButton(onClick = {
                    generateJob?.cancel()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { startGenerate() },
                        enabled = !isSaving
                    ) {
                        Text(stringResource(R.string.chat_page_summarize_window_regenerate))
                    }
                    TextButton(
                        onClick = {
                            saveJob = onSave(summary) { onDismiss() }
                        },
                        enabled = !isSaving && summary.isNotBlank()
                    ) {
                        Text(stringResource(R.string.chat_page_summarize_window_save))
                    }
                }
            }
        },
        dismissButton = {
            if (!busy) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
