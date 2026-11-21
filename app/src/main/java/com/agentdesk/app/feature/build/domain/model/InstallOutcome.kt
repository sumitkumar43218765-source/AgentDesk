package com.agentdesk.app.feature.build.domain.model

import android.content.Intent

sealed interface InstallOutcome {
    data object Success : InstallOutcome
    data class NeedsPermission(val settingsIntent: Intent) : InstallOutcome
    data class MismatchSignature(val packageName: String) : InstallOutcome
    data class Failure(val message: String) : InstallOutcome
}
