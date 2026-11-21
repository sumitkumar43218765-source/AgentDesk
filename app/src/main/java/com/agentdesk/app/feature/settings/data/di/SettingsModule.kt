package com.agentdesk.app.feature.settings.data.di

import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.core.notification.AppNotifier
import com.agentdesk.app.feature.settings.data.AndroidPermissionChecker
import com.agentdesk.app.feature.settings.domain.AppNotifierImpl
import com.agentdesk.app.feature.settings.domain.PermissionChecker
import com.agentdesk.app.feature.settings.presentation.SettingsNavGraphRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindAppNotifier(impl: AppNotifierImpl): AppNotifier

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker

    @Binds
    @IntoSet
    abstract fun bindSettingsNavGraphRegistrar(impl: SettingsNavGraphRegistrar): NavGraphRegistrar
}
