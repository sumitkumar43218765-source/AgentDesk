package com.agentdesk.app.core.di

import com.agentdesk.app.core.common.AndroidStringProvider
import com.agentdesk.app.core.common.Clock
import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.core.common.StringProvider
import com.agentdesk.app.core.common.SystemClock
import com.agentdesk.app.core.common.UuidGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindClock(clock: SystemClock): Clock

    @Binds
    @Singleton
    abstract fun bindIdGenerator(generator: UuidGenerator): IdGenerator

    @Binds
    @Singleton
    abstract fun bindStringProvider(provider: AndroidStringProvider): StringProvider
}
