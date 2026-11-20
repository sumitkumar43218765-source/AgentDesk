package com.agentdesk.app.feature.chat.domain

import androidx.compose.runtime.Composable
import com.agentdesk.app.feature.ai.domain.model.ContentBlock

interface ToolCallCardRenderer {
    fun canRender(block: ContentBlock.ToolUse): Boolean

    @Composable
    fun Render(block: ContentBlock.ToolUse, result: ContentBlock.ToolResult?)
}
