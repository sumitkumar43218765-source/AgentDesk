package com.agentdesk.app.feature.projects.data.di

import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.feature.agent.data.integration.AskAgentAboutFileImpl
import com.agentdesk.app.feature.projects.data.RoomProjectRepository
import com.agentdesk.app.feature.projects.data.fs.DefaultProjectFileSystem
import com.agentdesk.app.feature.projects.data.git.ShellGitService
import com.agentdesk.app.feature.projects.domain.repository.AskAgentAboutFile
import com.agentdesk.app.feature.projects.domain.repository.GitService
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import com.agentdesk.app.feature.projects.presentation.ProjectsNavGraphRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectsModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: RoomProjectRepository
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindProjectFileSystem(
        impl: DefaultProjectFileSystem
    ): ProjectFileSystem

    @Binds
    @Singleton
    abstract fun bindGitService(
        impl: ShellGitService
    ): GitService

    @Binds
    @Singleton
    abstract fun bindAskAgentAboutFile(
        impl: AskAgentAboutFileImpl
    ): AskAgentAboutFile

    @Binds
    @IntoSet
    abstract fun bindProjectsNavGraphRegistrar(
        impl: ProjectsNavGraphRegistrar
    ): NavGraphRegistrar
}
