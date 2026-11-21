package com.agentdesk.app.feature.settings.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_SERVICE = "service"
    const val CHANNEL_SETUP = "setup"
    const val CHANNEL_BUILD = "build"
    const val CHANNEL_AGENT = "agent"
    const val CHANNEL_APPROVAL = "approval"
    const val CHANNEL_BACKUP = "backup"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Ongoing Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Foreground ongoing operations (setup, build, agent runtime)" }

            val setupChannel = NotificationChannel(
                CHANNEL_SETUP,
                "Tool Setup",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Tool installations and downloads completed or failed" }

            val buildChannel = NotificationChannel(
                CHANNEL_BUILD,
                "Build Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "APK build results and installation triggers" }

            val agentChannel = NotificationChannel(
                CHANNEL_AGENT,
                "Agent Runs",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Agent task completions and interruptions" }

            val approvalChannel = NotificationChannel(
                CHANNEL_APPROVAL,
                "Agent Approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "High-priority user permission approval requests" }

            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "Backup Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Project backup reminder notifications" }

            manager.createNotificationChannels(
                listOf(serviceChannel, setupChannel, buildChannel, agentChannel, approvalChannel, backupChannel)
            )
        }
    }
}
