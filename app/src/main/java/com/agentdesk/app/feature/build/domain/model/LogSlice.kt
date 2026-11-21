package com.agentdesk.app.feature.build.domain.model

data class LogSlice(
    val lines: List<String>,
    val totalLines: Int,
    val offset: Int,
    val hasMore: Boolean
)
