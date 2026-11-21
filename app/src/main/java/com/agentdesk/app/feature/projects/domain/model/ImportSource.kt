package com.agentdesk.app.feature.projects.domain.model

import java.io.File
import java.io.InputStream

sealed interface ImportSource {
    data class ZipFile(val streamSupplier: () -> InputStream, val suggestedName: String) : ImportSource
    data class SharedFolder(val folder: File, val suggestedName: String) : ImportSource
}
