package com.agentdesk.app.feature.chat.data.di

import com.agentdesk.app.core.database.AppDatabase
import com.agentdesk.app.feature.chat.data.local.ConversationDao
import com.agentdesk.app.feature.chat.data.local.MessageDao
import com.agentdesk.app.feature.chat.data.local.RoomChatRepository
import com.agentdesk.app.feature.chat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(repo: RoomChatRepository): ChatRepository

    companion object {
        @Provides
        fun provideConversationDao(database: AppDatabase): ConversationDao {
            return database.conversationDao()
        }

        @Provides
        fun provideMessageDao(database: AppDatabase): MessageDao {
            return database.messageDao()
        }
    }
}
