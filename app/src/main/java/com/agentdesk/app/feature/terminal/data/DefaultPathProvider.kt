package com.agentdesk.app.feature.terminal.data

import android.content.Context
import com.agentdesk.app.feature.terminal.domain.PathProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPathProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : PathProvider {

    override val termuxPrefix: File by lazy {
        File(context.filesDir, "usr").apply { if (!exists()) mkdirs() }
    }

    override val termuxHome: File by lazy {
        File(context.filesDir, "home").apply { if (!exists()) mkdirs() }
    }

    override val tmpDir: File by lazy {
        File(termuxPrefix, "tmp").apply { if (!exists()) mkdirs() }
    }

    override val rootfsDir: File by lazy {
        File(context.filesDir, "rootfs/ubuntu").apply { if (!exists()) mkdirs() }
    }

    override val rootfsHome: File by lazy {
        File(rootfsDir, "root").apply { if (!exists()) mkdirs() }
    }

    override val projectsRoot: File by lazy {
        File(rootfsHome, "projects").apply { if (!exists()) mkdirs() }
    }

    override val setupStateDir: File by lazy {
        File(context.filesDir, "setup").apply { if (!exists()) mkdirs() }
    }

    override val logsDir: File by lazy {
        File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
    }

    override val buildLogsDir: File by lazy {
        File(logsDir, "builds").apply { if (!exists()) mkdirs() }
    }

    override fun hostToGuest(host: File): String? {
        val rootfsPath = rootfsDir.absolutePath
        val hostPath = host.absolutePath

        return if (hostPath.startsWith(rootfsPath)) {
            val relative = hostPath.removePrefix(rootfsPath)
            if (relative.startsWith("/")) relative else "/$relative"
        } else {
            null
        }
    }

    override fun guestToHost(guest: String): File {
        val cleanGuest = guest.trimStart('/')
        return File(rootfsDir, cleanGuest)
    }
}
