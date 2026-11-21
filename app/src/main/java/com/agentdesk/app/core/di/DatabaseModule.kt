package com.agentdesk.app.core.di

import android.content.Context
import androidx.room.Room
import com.agentdesk.app.core.database.AppDatabase
import com.agentdesk.app.core.database.AppMetadataDao
import com.agentdesk.app.feature.build.data.local.BuildRunDao
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import com.agentdesk.app.feature.setup.data.local.ToolInstallStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "agentdesk.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAppMetadataDao(database: AppDatabase): AppMetadataDao {
        return database.appMetadataDao()
    }

    @Provides
    fun provideToolInstallStateDao(database: AppDatabase): ToolInstallStateDao {
        return database.toolInstallStateDao()
    }

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideBuildRunDao(database: AppDatabase): BuildRunDao {
        return database.buildRunDao()
    }

    @Provides
    fun provideAgentRunDao(database: AppDatabase): com.agentdesk.app.feature.agent.data.local.AgentRunDao {
        return database.agentRunDao()
    }

    @Provides
    fun providePermissionGrantDao(database: AppDatabase): com.agentdesk.app.feature.agent.data.local.PermissionGrantDao {
        return database.permissionGrantDao()
    }
}
