package com.agentdesk.app.core.di

import android.content.Context
import androidx.room.Room
import com.agentdesk.app.core.database.AppDatabase
import com.agentdesk.app.core.database.AppMetadataDao
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
}
