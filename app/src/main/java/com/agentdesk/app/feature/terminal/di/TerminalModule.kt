package com.agentdesk.app.feature.terminal.di

import com.agentdesk.app.feature.terminal.data.DefaultEnvironmentProbe
import com.agentdesk.app.feature.terminal.data.DefaultPathProvider
import com.agentdesk.app.feature.terminal.data.EmbeddedShellBackend
import com.agentdesk.app.feature.terminal.data.TermuxBootstrapGate
import com.agentdesk.app.feature.terminal.domain.BootstrapGate
import com.agentdesk.app.feature.terminal.domain.EnvironmentProbe
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TerminalModule {

    @Binds
    @Singleton
    abstract fun bindPathProvider(provider: DefaultPathProvider): PathProvider

    @Binds
    @Singleton
    abstract fun bindShellBackend(backend: EmbeddedShellBackend): ShellBackend

    @Binds
    @Singleton
    abstract fun bindBootstrapGate(gate: TermuxBootstrapGate): BootstrapGate

    @Binds
    @Singleton
    abstract fun bindEnvironmentProbe(probe: DefaultEnvironmentProbe): EnvironmentProbe
}
