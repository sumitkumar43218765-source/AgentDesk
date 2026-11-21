package com.agentdesk.app.feature.agent.presentation.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agentdesk.app.feature.ai.domain.model.ContentBlock

@Composable
fun ToolCardRegistrar(
    toolUse: ContentBlock.ToolUse,
    toolResult: ContentBlock.ToolResult?,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    ToolCallCard(
        toolName = toolUse.name,
        argsSummary = toolUse.argsJson,
        output = toolResult?.content,
        isError = toolResult?.isError ?: false,
        isRunning = isRunning,
        modifier = modifier
    )
}
