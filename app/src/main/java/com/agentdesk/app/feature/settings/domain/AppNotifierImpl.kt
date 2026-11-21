package com.agentdesk.app.feature.settings.domain

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.agentdesk.app.core.notification.AppNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppNotifier {

    override fun createChannels() {
        NotificationChannels.createChannels(context)
    }

    override fun notify(notificationId: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handled on newer Android platforms without notification permission
        }
    }

    override fun cancel(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
