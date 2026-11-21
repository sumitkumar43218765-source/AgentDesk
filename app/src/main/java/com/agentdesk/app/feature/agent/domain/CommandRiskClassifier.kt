package com.agentdesk.app.feature.agent.domain

import com.agentdesk.app.feature.agent.domain.model.RiskLevel

interface CommandRiskClassifier {
    fun classify(command: String, cwd: String = ""): RiskLevel
}
