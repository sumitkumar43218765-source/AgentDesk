package com.agentdesk.app.feature.chat.domain.model

import com.agentdesk.app.core.common.AppError

sealed interface StreamingState {
    data object Idle : StreamingState
    data class Streaming(val partialText: String, val messageId: String) : StreamingState
    data class Completed(val messageId: String) : StreamingState
    data class Failed(val error: AppError, val messageId: String) : StreamingState
}
