package com.agentdesk.app.feature.chat.domain.usecase

import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class RecoverInterruptedUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke() {
        repository.recoverInterruptedMessages()
    }
}
