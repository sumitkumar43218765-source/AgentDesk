package com.agentdesk.app.feature.build.data.di

import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.feature.build.data.check.HelloWorldSmokeTest
import com.agentdesk.app.feature.build.data.engine.DefaultBuildEngine
import com.agentdesk.app.feature.build.data.install.MediaStoreApkExporter
import com.agentdesk.app.feature.build.data.install.PackageInstallerApkInstaller
import com.agentdesk.app.feature.build.data.sign.ShellKeystoreManager
import com.agentdesk.app.feature.build.domain.ApkExporter
import com.agentdesk.app.feature.build.domain.ApkInstaller
import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.DefaultFixWithAiLauncher
import com.agentdesk.app.feature.build.domain.FixWithAiLauncher
import com.agentdesk.app.feature.build.domain.KeystoreManager
import com.agentdesk.app.feature.build.presentation.BuildNavGraphRegistrar
import com.agentdesk.app.feature.setup.domain.PostInstallCheck
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BuildModule {

    @Binds
    @Singleton
    abstract fun bindBuildEngine(engine: DefaultBuildEngine): BuildEngine

    @Binds
    @Singleton
    abstract fun bindApkInstaller(installer: PackageInstallerApkInstaller): ApkInstaller

    @Binds
    @Singleton
    abstract fun bindApkExporter(exporter: MediaStoreApkExporter): ApkExporter

    @Binds
    @Singleton
    abstract fun bindFixWithAiLauncher(launcher: DefaultFixWithAiLauncher): FixWithAiLauncher

    @Binds
    @Singleton
    abstract fun bindKeystoreManager(manager: ShellKeystoreManager): KeystoreManager

    @Binds
    @IntoSet
    abstract fun bindHelloWorldSmokeTest(check: HelloWorldSmokeTest): PostInstallCheck

    @Binds
    @IntoSet
    abstract fun bindBuildNavGraphRegistrar(registrar: BuildNavGraphRegistrar): NavGraphRegistrar
}
