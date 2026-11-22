package com.agentdesk.app.termux

import android.content.Context
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object TermuxInstaller {

    private const val TAG = "TermuxInstaller"

    /**
     * Extracts a Termux bootstrap ZIP stream into destination prefix directory,
     * processing SYMLINKS.txt and setting executable permissions.
     */
    fun setupBootstrapFromStream(
        zipInputStream: InputStream,
        prefixDir: File,
        stagingDir: File,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> {
        try {
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
            stagingDir.mkdirs()

            val symlinks = mutableListOf<Pair<String, String>>()
            val buffer = ByteArray(8192)

            ZipInputStream(zipInputStream).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry ?: break
                    val entryName = currentEntry.name

                    if (entryName == "SYMLINKS.txt") {
                        val reader = BufferedReader(InputStreamReader(zis))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val parts = line!!.split("←")
                            if (parts.size == 2) {
                                symlinks.add(parts[0] to parts[1])
                            }
                        }
                        continue
                    }

                    val destFile = File(stagingDir, entryName)
                    if (currentEntry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }

                        // Make binaries executable
                        if (entryName.startsWith("bin/") ||
                            entryName.startsWith("libexec/") ||
                            entryName.startsWith("lib/apt/") ||
                            entryName.endsWith(".sh")
                        ) {
                            try {
                                Os.chmod(destFile.absolutePath, 448 /* 0700 */)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed chmod 0700 on ${destFile.name}: ${e.message}")
                            }
                        }
                    }
                }
            }

            // Create symlinks
            for ((oldPath, symlinkPath) in symlinks) {
                val symlinkFile = File(stagingDir, symlinkPath)
                symlinkFile.parentFile?.mkdirs()
                if (symlinkFile.exists()) {
                    symlinkFile.delete()
                }
                try {
                    Os.symlink(oldPath, symlinkFile.absolutePath)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create symlink $symlinkPath -> $oldPath: ${e.message}")
                }
            }

            // Move staging to prefix
            if (prefixDir.exists()) {
                prefixDir.deleteRecursively()
            }
            prefixDir.parentFile?.mkdirs()

            val renamed = stagingDir.renameTo(prefixDir)
            if (!renamed) {
                stagingDir.copyRecursively(prefixDir, overwrite = true)
                stagingDir.deleteRecursively()
            }

            onProgress(1.0f)
            return AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap extraction failed", e)
            stagingDir.deleteRecursively()
            return AppResult.Failure(AppError.Storage("Bootstrap extraction failed: ${e.message}"))
        }
    }
}
