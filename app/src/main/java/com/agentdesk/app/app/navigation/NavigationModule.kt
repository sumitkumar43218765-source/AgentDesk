package com.agentdesk.app.app.navigation

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
}
