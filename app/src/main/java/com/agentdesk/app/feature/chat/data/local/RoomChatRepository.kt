package com.agentdesk.app.feature.chat.data.local

import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import com.agentdesk.app.feature.chat.domain.model.Conversation
import com.agentdesk.app.feature.chat.domain.model.MessageStatus
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val converter: BlocksJsonConverter
) : ChatRepository {

    override fun observeConversations(): Flow<List<Conversation>> {
        return conversationDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getConversation(id: String): Conversation? {
        return conversationDao.getById(id)?.toDomain()
    }

    override suspend fun saveConversation(conversation: Conversation) {
        conversationDao.insert(conversation.toEntity())
    }

    override suspend fun updateConversation(conversation: Conversation) {
        conversationDao.update(conversation.toEntity())
    }

    override suspend fun deleteConversation(id: String) {
        messageDao.deleteByConversation(id)
        conversationDao.deleteById(id)
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.observeByConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMessage(id: String): ChatMessage? {
        return messageDao.getById(id)?.toDomain()
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messageDao.insert(message.toEntity())
    }

    override suspend fun updateMessage(message: ChatMessage) {
        messageDao.update(message.toEntity())
    }

    override suspend fun deleteMessage(id: String) {
        messageDao.deleteById(id)
    }

    override suspend fun recoverInterruptedMessages() {
        messageDao.markStreamingAsInterrupted()
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        projectId = projectId,
        title = title,
        providerId = providerId,
        model = model,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pinned = pinned
    )

    private fun Conversation.toEntity() = ConversationEntity(
        id = id,
        projectId = projectId,
        title = title,
        providerId = providerId,
        model = model,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pinned = pinned
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = if (role == "USER") Role.USER else Role.ASSISTANT,
        blocks = converter.jsonToBlocks(blocksJson),
        status = MessageStatus.valueOf(status),
        usage = converter.jsonToUsage(usageJson),
        createdAt = createdAt
    )

    private fun ChatMessage.toEntity() = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        blocksJson = converter.blocksToJson(blocks),
        status = status.name,
        usageJson = converter.usageToJson(usage),
        createdAt = createdAt
    )
}
