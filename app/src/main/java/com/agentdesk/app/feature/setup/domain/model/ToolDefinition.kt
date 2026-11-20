package com.agentdesk.app.feature.setup.domain.model

import com.agentdesk.app.feature.terminal.domain.ShellEnvironment

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val required: Boolean,
    val dependsOn: List<String>,
    val sizeEstimateMb: Long,
    val environment: ShellEnvironment,
    val source: ToolSource,
    val install: List<String>,
    val verify: String,
    val version: String
)
