package com.agentdesk.app.core.di

import com.agentdesk.app.core.logging.AppLogger
import com.agentdesk.app.core.logging.FileAppLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Binds
    @Singleton
    abstract fun bindAppLogger(logger: FileAppLogger): AppLogger
}
