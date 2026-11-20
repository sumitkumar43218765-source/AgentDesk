package com.agentdesk.app.feature.ai.domain.model

sealed interface ContentBlock {
    data class Text(val text: String) : ContentBlock
    data class ToolUse(val id: String, val name: String, val argsJson: String) : ContentBlock
    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : ContentBlock
}
