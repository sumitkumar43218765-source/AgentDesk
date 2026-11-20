package com.agentdesk.app.feature.terminal.domain

import java.io.File

interface PathProvider {
    val termuxPrefix: File
    val termuxHome: File
    val tmpDir: File
    val rootfsDir: File
    val rootfsHome: File
    val projectsRoot: File
    val setupStateDir: File
    val logsDir: File
    val buildLogsDir: File

    fun hostToGuest(host: File): String?
    fun guestToHost(guest: String): File
}
