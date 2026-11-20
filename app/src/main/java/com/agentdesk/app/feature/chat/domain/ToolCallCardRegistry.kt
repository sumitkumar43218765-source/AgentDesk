package com.agentdesk.app.feature.chat.domain

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolCallCardRegistry @Inject constructor(
    private val renderers: Set<@JvmSuppressWildcards ToolCallCardRenderer>
) {
    fun getRenderer(block: ContentBlock.ToolUse): ToolCallCardRenderer? {
        return renderers.firstOrNull { it.canRender(block) }
    }
}
