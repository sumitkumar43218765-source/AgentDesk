package com.agentdesk.app.feature.ai.data.http

data class SseEvent(
    val event: String? = null,
    val data: String = "",
    val id: String? = null
)
