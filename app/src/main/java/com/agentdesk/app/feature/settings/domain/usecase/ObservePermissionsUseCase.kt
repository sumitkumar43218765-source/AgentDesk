package com.agentdesk.app.feature.settings.domain.usecase

import com.agentdesk.app.feature.settings.domain.PermissionChecker
import com.agentdesk.app.feature.settings.domain.model.PermissionItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePermissionsUseCase @Inject constructor(
    private val permissionChecker: PermissionChecker
) {
    operator fun invoke(): Flow<List<PermissionItem>> {
        return permissionChecker.observePermissions()
    }
}
