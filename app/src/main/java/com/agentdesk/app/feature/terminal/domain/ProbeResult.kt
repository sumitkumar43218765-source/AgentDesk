package com.agentdesk.app.feature.terminal.domain

data class ProbeResult(
    val name: String,
    val passed: Boolean,
    val version: String? = null,
    val path: String? = null,
    val error: String? = null
)
