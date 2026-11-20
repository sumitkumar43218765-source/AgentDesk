package com.agentdesk.app.app.navigation

import com.agentdesk.app.feature.ai.presentation.AiNavGraphRegistrar
import com.agentdesk.app.feature.chat.presentation.ChatNavGraphRegistrar
import com.agentdesk.app.feature.terminal.presentation.TerminalNavGraphRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds
    @IntoSet
    abstract fun bindHomeNavGraphRegistrar(registrar: HomeNavGraphRegistrar): NavGraphRegistrar

    @Binds
    @IntoSet
    abstract fun bindChatNavGraphRegistrar(registrar: ChatNavGraphRegistrar): NavGraphRegistrar

    @Binds
    @IntoSet
    abstract fun bindAiNavGraphRegistrar(registrar: AiNavGraphRegistrar): NavGraphRegistrar

    @Binds
    @IntoSet
    abstract fun bindTerminalNavGraphRegistrar(registrar: TerminalNavGraphRegistrar): NavGraphRegistrar
}
