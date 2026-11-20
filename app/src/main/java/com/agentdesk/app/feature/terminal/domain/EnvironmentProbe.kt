package com.agentdesk.app.feature.terminal.domain

interface EnvironmentProbe {
    suspend fun runProbes(): List<ProbeResult>
}
