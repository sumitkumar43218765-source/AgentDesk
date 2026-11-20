package com.agentdesk.app.core.di

import com.agentdesk.app.core.common.DispatcherProvider
import com.agentdesk.app.core.common.StandardDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DispatcherModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(provider: StandardDispatcherProvider): DispatcherProvider
}
