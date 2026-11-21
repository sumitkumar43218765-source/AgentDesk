package com.agentdesk.app.termux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.termux.terminal.TerminalSession
import java.util.concurrent.CopyOnWriteArrayList

class TermuxService : Service() {

    companion object {
        private const val CHANNEL_ID = "termux_service_channel"
        private const val NOTIFICATION_ID = 1337
    }

    inner class LocalBinder : Binder() {
        val service: TermuxService
            get() = this@TermuxService
    }

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    val sessions: MutableList<TerminalSession> = CopyOnWriteArrayList()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification("Terminal Service Running")
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    fun addSession(session: TerminalSession) {
        if (!sessions.contains(session)) {
            sessions.add(session)
        }
        acquireWakeLock()
    }

    fun removeSession(session: TerminalSession) {
        sessions.remove(session)
        try {
            session.finishIfRunning()
        } catch (ignored: Exception) {}
        if (sessions.isEmpty()) {
            releaseWakeLock()
        }
    }

    fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AgentDesk:TermuxWakeLock")
        }
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        for (session in sessions) {
            try {
                session.finishIfRunning()
            } catch (ignored: Exception) {}
        }
        sessions.clear()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Terminal Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AgentDesk Terminal")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setOngoing(true)
            .build()
    }
}
