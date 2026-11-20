package com.agentdesk.app.core.di

import com.agentdesk.app.core.security.KeystoreSecretStore
import com.agentdesk.app.core.security.SecretStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSecretStore(store: KeystoreSecretStore): SecretStore
}
