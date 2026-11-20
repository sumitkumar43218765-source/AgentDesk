package com.agentdesk.app.feature.ai.data.local

import com.agentdesk.app.feature.ai.domain.ToolRegistry
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmptyToolRegistry @Inject constructor() : ToolRegistry {
    override fun getTools(): List<ToolSpec> = emptyList()
}
