package com.agentdesk.app.feature.ai.domain.model

import com.agentdesk.app.core.common.AppError

sealed interface LlmEvent {
    data class TextDelta(val text: String) : LlmEvent
    data class ToolCallStarted(val id: String, val name: String) : LlmEvent
    data class ToolCallArgsDelta(val id: String, val partialJson: String) : LlmEvent
    data class ToolCallCompleted(val id: String, val name: String, val argsJson: String) : LlmEvent
    data class Usage(val inputTokens: Int, val outputTokens: Int) : LlmEvent
    data class Done(val stopReason: StopReason) : LlmEvent
    data class Error(val error: AppError) : LlmEvent
}
