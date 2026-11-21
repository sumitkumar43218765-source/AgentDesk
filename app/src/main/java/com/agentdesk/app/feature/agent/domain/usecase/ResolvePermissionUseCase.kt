package com.agentdesk.app.feature.agent.domain.usecase

import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import javax.inject.Inject

class ResolvePermissionUseCase @Inject constructor(
    private val permissionGateway: PermissionGateway
) {
    suspend operator fun invoke(requestId: String, decision: PermissionDecision) {
        permissionGateway.resolve(requestId, decision)
    }
}
