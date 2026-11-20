package com.agentdesk.app.feature.setup.data.install

import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoneMarkers @Inject constructor(
    private val pathProvider: PathProvider
) {
    private val doneDir: File
        get() = File(pathProvider.setupStateDir, "done").apply {
            if (!exists()) mkdirs()
        }

    fun isDone(toolId: String, version: String): Boolean {
        val marker = File(doneDir, "$toolId@$version")
        return marker.exists()
    }

    fun markDone(toolId: String, version: String) {
        val marker = File(doneDir, "$toolId@$version")
        marker.createNewFile()
    }

    fun removeMarker(toolId: String, version: String? = null) {
        if (version != null) {
            File(doneDir, "$toolId@$version").delete()
        } else {
            doneDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("$toolId@")) {
                    file.delete()
                }
            }
        }
    }

    fun getDoneMarkerPath(toolId: String, version: String): String {
        return File(doneDir, "$toolId@$version").absolutePath
    }
}
