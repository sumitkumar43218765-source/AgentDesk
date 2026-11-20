package com.agentdesk.app.feature.setup.domain.model

data class SetupStatusEvent(
    val ts: Long,
    val tool: String,
    val event: String,
    val pct: Int = 0,
    val msg: String = ""
)
