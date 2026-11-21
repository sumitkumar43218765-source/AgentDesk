package com.agentdesk.app.feature.agent.data.permission

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agentdesk.app.app.MainActivity
import com.agentdesk.app.feature.agent.data.service.AgentRunService
import com.agentdesk.app.feature.agent.domain.model.PermissionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "agent_permission_channel"
        const val NOTIFICATION_ID = 2003
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Agent Permissions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Requires user approval to allow agent tools execution"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createNotification(request: PermissionRequest): Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pContentIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val allowIntent = Intent(context, AgentRunService::class.java).apply {
            action = AgentRunService.ACTION_PERMISSION_ALLOW
            putExtra(AgentRunService.EXTRA_REQUEST_ID, request.id)
        }
        val pAllowIntent = PendingIntent.getService(
            context,
            1,
            allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val denyIntent = Intent(context, AgentRunService::class.java).apply {
            action = AgentRunService.ACTION_PERMISSION_DENY
            putExtra(AgentRunService.EXTRA_REQUEST_ID, request.id)
        }
        val pDenyIntent = PendingIntent.getService(
            context,
            2,
            denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Approval needed: ${request.summary.title}")
            .setContentText(request.summary.details ?: "The agent wants to execute ${request.toolName}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pContentIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Allow", pAllowIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deny", pDenyIntent)
            .setAutoCancel(true)
            .build()
    }
}
