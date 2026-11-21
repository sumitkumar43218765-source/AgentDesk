package com.agentdesk.app.feature.agent.domain.model

enum class RunStatus {
    IDLE,
    RUNNING,
    INTERRUPTED,
    WAITING_FOR_PERMISSION,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED
}
