package com.agentdesk.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agentdesk.app.feature.ai.data.local.ProviderConfigDao
import com.agentdesk.app.feature.ai.data.local.ProviderConfigEntity
import com.agentdesk.app.feature.build.data.local.BuildRunDao
import com.agentdesk.app.feature.build.data.local.BuildRunEntity
import com.agentdesk.app.feature.chat.data.local.ConversationDao
import com.agentdesk.app.feature.chat.data.local.ConversationEntity
import com.agentdesk.app.feature.chat.data.local.MessageDao
import com.agentdesk.app.feature.chat.data.local.MessageEntity
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import com.agentdesk.app.feature.projects.data.local.ProjectEntity
import com.agentdesk.app.feature.setup.data.local.ToolInstallStateDao
import com.agentdesk.app.feature.setup.data.local.ToolInstallStateEntity

@Database(
    entities = [
        AppMetadataEntity::class,
        ProviderConfigEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ToolInstallStateEntity::class,
        ProjectEntity::class,
        BuildRunEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun providerConfigDao(): ProviderConfigDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun toolInstallStateDao(): ToolInstallStateDao
    abstract fun projectDao(): ProjectDao
    abstract fun buildRunDao(): BuildRunDao
}
