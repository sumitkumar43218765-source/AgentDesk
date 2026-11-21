package com.agentdesk.app.feature.settings.domain.model

import android.content.Intent

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val state: PermissionState,
    val actionIntent: Intent? = null
)
