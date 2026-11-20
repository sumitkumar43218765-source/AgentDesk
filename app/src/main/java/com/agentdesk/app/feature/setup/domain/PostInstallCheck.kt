package com.agentdesk.app.feature.setup.domain

import com.agentdesk.app.feature.setup.domain.model.CheckResult

interface PostInstallCheck {
    val id: String
    val title: String
    suspend fun run(): CheckResult
}
