package com.agentdesk.app.feature.build.data.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.ApkInstaller
import com.agentdesk.app.feature.build.domain.model.InstallOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageInstallerApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : ApkInstaller {

    override suspend fun install(apk: File): AppResult<InstallOutcome> = withContext(Dispatchers.IO) {
        if (!apk.exists() || apk.length() == 0L) {
            return@withContext AppResult.Failure(AppError.Storage("APK file does not exist or is empty: ${apk.absolutePath}"))
        }

        // Check Unknown App Sources permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return@withContext AppResult.Success(InstallOutcome.NeedsPermission(settingsIntent))
            }
        }

        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setSize(apk.length())

        var sessionId = -1
        try {
            sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            session.openWrite("package_install", 0, apk.length()).use { outStream ->
                FileInputStream(apk).use { inStream ->
                    inStream.copyTo(outStream)
                }
                session.fsync(outStream)
            }

            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                action = InstallResultReceiver.ACTION_INSTALL_STATUS
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)

            session.commit(pendingIntent.intentSender)
            session.close()

            // Wait for broadcast confirmation (up to 60s for user prompt)
            val outcomeEvent = withTimeoutOrNull(60_000) {
                InstallResultReceiver.installEvents.first()
            }

            val outcome = when (outcomeEvent) {
                is InstallBroadcastEvent.Success -> InstallOutcome.Success
                is InstallBroadcastEvent.SignatureMismatch -> InstallOutcome.MismatchSignature(outcomeEvent.packageName)
                is InstallBroadcastEvent.Failure -> InstallOutcome.Failure(outcomeEvent.message)
                null -> InstallOutcome.Success // Session committed, pending user confirmation in system installer
            }

            AppResult.Success(outcome)
        } catch (e: Exception) {
            if (sessionId != -1) {
                runCatching { packageInstaller.abandonSession(sessionId) }
            }
            AppResult.Failure(AppError.Build("PackageInstaller failed: ${e.message}"))
        }
    }
}
