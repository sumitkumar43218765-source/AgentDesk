package com.agentdesk.app.feature.chat.domain.repository

import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    suspend fun getConversation(id: String): Conversation?
    suspend fun saveConversation(conversation: Conversation)
    suspend fun updateConversation(conversation: Conversation)
    suspend fun deleteConversation(id: String)
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun getMessage(id: String): ChatMessage?
    suspend fun saveMessage(message: ChatMessage)
    suspend fun updateMessage(message: ChatMessage)
    suspend fun deleteMessage(id: String)
    suspend fun recoverInterruptedMessages()
}
