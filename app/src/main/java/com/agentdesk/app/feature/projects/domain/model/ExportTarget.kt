package com.agentdesk.app.feature.projects.domain.model

import java.io.OutputStream

sealed interface ExportTarget {
    data class Downloads(val fileName: String) : ExportTarget
    data class SafUri(val streamSupplier: () -> OutputStream) : ExportTarget
}
