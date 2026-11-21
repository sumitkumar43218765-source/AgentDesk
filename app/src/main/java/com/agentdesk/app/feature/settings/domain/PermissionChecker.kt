package com.agentdesk.app.feature.settings.domain

import com.agentdesk.app.feature.settings.domain.model.PermissionItem
import kotlinx.coroutines.flow.Flow

interface PermissionChecker {
    fun getPermissions(): List<PermissionItem>
    fun observePermissions(): Flow<List<PermissionItem>>
}
