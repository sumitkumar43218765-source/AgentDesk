package com.agentdesk.app.feature.build.domain.model

data class DistilledLog(
    val text: String,
    val totalOriginalLines: Int,
    val omittedLinesCount: Int,
    val errorCount: Int,
    val warningCount: Int
)
