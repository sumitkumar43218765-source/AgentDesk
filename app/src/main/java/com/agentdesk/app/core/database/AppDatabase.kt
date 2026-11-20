package com.agentdesk.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agentdesk.app.feature.ai.data.local.ProviderConfigDao
import com.agentdesk.app.feature.ai.data.local.ProviderConfigEntity
import com.agentdesk.app.feature.chat.data.local.ConversationDao
import com.agentdesk.app.feature.chat.data.local.ConversationEntity
import com.agentdesk.app.feature.chat.data.local.MessageDao
import com.agentdesk.app.feature.chat.data.local.MessageEntity

@Database(
    entities = [
        AppMetadataEntity::class,
        ProviderConfigEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun providerConfigDao(): ProviderConfigDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
