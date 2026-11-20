package com.agentdesk.app.feature.setup.data.di

import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.feature.setup.data.check.ToolVerifyCheck
import com.agentdesk.app.feature.setup.data.local.RoomToolStateRepository
import com.agentdesk.app.feature.setup.data.manifest.AssetToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.PostInstallCheck
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.setup.presentation.SetupNavGraphRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SetupModule {

    @Binds
    @Singleton
    abstract fun bindToolCatalogRepository(
        impl: AssetToolCatalogRepository
    ): ToolCatalogRepository

    @Binds
    @Singleton
    abstract fun bindToolStateRepository(
        impl: RoomToolStateRepository
    ): ToolStateRepository

    @Binds
    @IntoSet
    abstract fun bindToolVerifyCheck(
        impl: ToolVerifyCheck
    ): PostInstallCheck

    @Binds
    @IntoSet
    abstract fun bindSetupNavGraphRegistrar(
        impl: SetupNavGraphRegistrar
    ): NavGraphRegistrar
}
