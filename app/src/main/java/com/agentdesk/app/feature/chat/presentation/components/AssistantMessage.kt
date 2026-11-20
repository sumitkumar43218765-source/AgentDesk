package com.agentdesk.app.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agentdesk.app.R
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.MessageStatus

@Composable
fun AssistantMessage(
    message: ChatMessage,
    onContinue: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 32.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column {
                for (block in message.blocks) {
                    when (block) {
                        is ContentBlock.Text -> {
                            MarkdownText(markdown = block.text)
                        }
                        is ContentBlock.ToolUse -> {
                            GenericToolCard(toolUse = block)
                        }
                        is ContentBlock.ToolResult -> {}
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (message.status == MessageStatus.STREAMING) {
                    Text(
                        text = "●",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (message.status == MessageStatus.INTERRUPTED) {
                    StatusChip(
                        text = stringResource(R.string.interrupted_notice),
                        level = StatusLevel.WARNING
                    )
                    if (onContinue != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = onContinue) {
                            Text(stringResource(R.string.continue_action))
                        }
                    }
                }

                if (message.status == MessageStatus.ERROR) {
                    StatusChip(text = "Error generating response", level = StatusLevel.ERROR)
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }

        if (message.usage != null) {
            UsageFootnote(usage = message.usage)
        }
    }
}
