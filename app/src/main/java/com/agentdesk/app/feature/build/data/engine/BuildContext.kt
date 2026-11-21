package com.agentdesk.app.feature.build.data.engine

import com.agentdesk.app.feature.build.data.parse.ManifestInfo
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.SigningConfig
import java.io.File

data class BuildContext(
    val projectId: String,
    val projectSlug: String,
    val projectDir: File,
    val guestProjectDir: String,
    val buildDir: File,
    val manifestInfo: ManifestInfo,
    val variant: BuildVariant,
    val signingConfig: SigningConfig,
    val androidJarGuestPath: String,
    val buildToolsGuestDir: String,
    val javaHomeGuestPath: String
)
