package com.agentdesk.app.feature.ai.domain

import com.agentdesk.app.feature.ai.domain.model.ToolSpec

interface ToolRegistry {
    fun getTools(): List<ToolSpec>
}
