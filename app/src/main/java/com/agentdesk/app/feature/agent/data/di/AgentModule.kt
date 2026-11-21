package com.agentdesk.app.feature.agent.data.di

import com.agentdesk.app.feature.agent.data.permission.DefaultCommandRiskClassifier
import com.agentdesk.app.feature.agent.data.permission.DefaultPermissionGateway
import com.agentdesk.app.feature.agent.data.prompt.DefaultSystemPromptBuilder
import com.agentdesk.app.feature.agent.data.runtime.DefaultAgentLoop
import com.agentdesk.app.feature.agent.data.runtime.DefaultAgentRunner
import com.agentdesk.app.feature.agent.data.runtime.DefaultContextManager
import com.agentdesk.app.feature.agent.domain.AgentLoop
import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.CommandRiskClassifier
import com.agentdesk.app.feature.agent.domain.ContextManager
import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.SystemPromptBuilder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {

    @Binds
    @Singleton
    abstract fun bindAgentRunner(runner: DefaultAgentRunner): AgentRunner

    @Binds
    @Singleton
    abstract fun bindAgentLoop(loop: DefaultAgentLoop): AgentLoop

    @Binds
    @Singleton
    abstract fun bindPermissionGateway(gateway: DefaultPermissionGateway): PermissionGateway

    @Binds
    @Singleton
    abstract fun bindCommandRiskClassifier(classifier: DefaultCommandRiskClassifier): CommandRiskClassifier

    @Binds
    @Singleton
    abstract fun bindContextManager(manager: DefaultContextManager): ContextManager

    @Binds
    @Singleton
    abstract fun bindSystemPromptBuilder(builder: DefaultSystemPromptBuilder): SystemPromptBuilder
}
