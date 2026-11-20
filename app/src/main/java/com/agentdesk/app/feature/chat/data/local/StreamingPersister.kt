package com.agentdesk.app.feature.chat.data.local

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.MessageStatus
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingPersister @Inject constructor(
    private val repository: ChatRepository
) {
    private val lastPersistedAt = AtomicLong(0L)
    private val PERSIST_INTERVAL_MS = 2000L

    suspend fun persistChunkIfNeeded(message: ChatMessage, currentPartialText: String) {
        val now = System.currentTimeMillis()
        if (now - lastPersistedAt.get() >= PERSIST_INTERVAL_MS) {
            lastPersistedAt.set(now)
            val updated = message.copy(
                blocks = listOf(ContentBlock.Text(currentPartialText)),
                status = MessageStatus.STREAMING
            )
            repository.updateMessage(updated)
        }
    }

    fun reset() {
        lastPersistedAt.set(0L)
    }
}
