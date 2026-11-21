package com.agentdesk.app.core.notification

import android.app.Notification

interface AppNotifier {
    fun createChannels()
    fun notify(notificationId: Int, notification: Notification)
    fun cancel(notificationId: Int)
}
