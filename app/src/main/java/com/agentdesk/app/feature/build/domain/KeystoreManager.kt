package com.agentdesk.app.feature.build.domain

import android.net.Uri
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.SigningConfig

interface KeystoreManager {
    suspend fun getOrCreateDebugKeystore(): AppResult<SigningConfig>
    suspend fun importReleaseKeystore(sourceUri: Uri, alias: String, storePass: String, keyPass: String): AppResult<SigningConfig>
    suspend fun getSigningConfig(variant: BuildVariant): AppResult<SigningConfig>
}
