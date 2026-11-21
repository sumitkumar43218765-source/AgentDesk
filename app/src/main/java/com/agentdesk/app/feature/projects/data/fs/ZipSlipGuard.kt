package com.agentdesk.app.feature.projects.data.fs

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZipSlipGuard @Inject constructor() {

    fun isValidEntry(destinationDir: File, entryName: String): Boolean {
        if (entryName.contains("../") || entryName.contains("..\\") || entryName.startsWith("/") || entryName.startsWith("\\")) {
            return false
        }

        return try {
            val canonicalDest = destinationDir.canonicalPath
            val target = File(destinationDir, entryName)
            val canonicalTarget = target.canonicalPath

            canonicalTarget == canonicalDest || canonicalTarget.startsWith(canonicalDest + File.separator)
        } catch (_: Exception) {
            false
        }
    }
}
