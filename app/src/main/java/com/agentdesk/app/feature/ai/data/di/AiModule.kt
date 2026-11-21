package com.agentdesk.app.feature.ai.data.di

import com.agentdesk.app.core.database.AppDatabase
import com.agentdesk.app.feature.agent.data.tools.ToolRegistryImpl
import com.agentdesk.app.feature.ai.data.local.DefaultProviderRegistry
import com.agentdesk.app.feature.ai.data.local.ProviderConfigDao
import com.agentdesk.app.feature.ai.data.local.RoomProviderConfigRepository
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.ToolRegistry
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindProviderConfigRepository(
        repo: RoomProviderConfigRepository
    ): ProviderConfigRepository

    @Binds
    @Singleton
    abstract fun bindProviderRegistry(
        registry: DefaultProviderRegistry
    ): ProviderRegistry

    @Binds
    @Singleton
    abstract fun bindToolRegistry(
        registry: ToolRegistryImpl
    ): ToolRegistry

    companion object {
        @Provides
        fun provideProviderConfigDao(database: AppDatabase): ProviderConfigDao {
            return database.providerConfigDao()
        }
    }
}
