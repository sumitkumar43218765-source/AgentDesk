package com.agentdesk.app.feature.settings.data

import android.content.Context
import android.os.Build
import com.agentdesk.app.BuildConfig
import com.agentdesk.app.core.logging.LogRedactor
import com.agentdesk.app.feature.settings.domain.model.DiagnosticsBundle
import com.agentdesk.app.feature.terminal.domain.PathProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pathProvider: PathProvider,
    private val logRedactor: LogRedactor
) {
    fun collect(): DiagnosticsBundle {
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val androidVersion = Build.VERSION.SDK_INT
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"

        val rawLogs = mutableListOf<String>()
        val logFiles = pathProvider.logsDir.listFiles() ?: emptyArray()
        for (f in logFiles.take(10)) {
            if (f.isFile) {
                rawLogs.add(f.absolutePath)
            }
        }

        val buildLogFiles = pathProvider.buildLogsDir.listFiles() ?: emptyArray()
        for (f in buildLogFiles.take(10)) {
            if (f.isFile) {
                rawLogs.add(f.absolutePath)
            }
        }

        val envInfo = buildString {
            append("App Version: $appVersion\n")
            append("Android SDK: $androidVersion\n")
            append("Device: $deviceModel\n")
            append("Internal Storage Free: ${context.filesDir.freeSpace / (1024 * 1024)} MB\n")
            append("Projects Root: ${pathProvider.projectsRoot.absolutePath}\n")
            append("Setup State Dir: ${pathProvider.setupStateDir.absolutePath}\n")
        }

        val redactedSummary = logRedactor.redact(envInfo)

        return DiagnosticsBundle(
            timestamp = System.currentTimeMillis(),
            appVersion = appVersion,
            androidVersion = androidVersion,
            deviceModel = deviceModel,
            logFiles = rawLogs,
            environmentSummary = redactedSummary
        )
    }
}
