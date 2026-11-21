package com.agentdesk.app.feature.build.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agentdesk.app.R
import com.agentdesk.app.app.MainActivity
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "build_service_channel"
        const val NOTIFICATION_ID = 2002
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Build Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live build progress for Android APK compilation"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        projectName: String,
        currentStep: BuildStepId?,
        progressIndex: Int,
        totalSteps: Int,
        buildId: String
    ): Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pContentIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val cancelIntent = Intent(context, BuildService::class.java).apply {
            action = BuildService.ACTION_CANCEL_BUILD
            putExtra(BuildService.EXTRA_BUILD_ID, buildId)
        }
        val pCancelIntent = PendingIntent.getService(
            context,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stepText = when (currentStep) {
            BuildStepId.RESOURCE_COMPILE -> "Compiling resources..."
            BuildStepId.RESOURCE_LINK -> "Linking resources with aapt2..."
            BuildStepId.JAVA_COMPILE -> "Compiling Java sources with ECJ..."
            BuildStepId.DEX -> "Dexing bytecode with D8..."
            BuildStepId.PACKAGE -> "Packaging APK..."
            BuildStepId.ALIGN -> "Aligning APK with zipalign..."
            BuildStepId.SIGN -> "Signing APK with apksigner..."
            null -> "Preparing build..."
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Building $projectName")
            .setContentText("$stepText ($progressIndex/$totalSteps)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setProgress(totalSteps, progressIndex, false)
            .setContentIntent(pContentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pCancelIntent)
            .build()
    }
}
