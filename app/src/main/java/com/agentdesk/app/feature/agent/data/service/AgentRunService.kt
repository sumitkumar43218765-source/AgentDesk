package com.agentdesk.app.feature.agent.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.agentdesk.app.feature.agent.domain.AgentRunner
import com.agentdesk.app.feature.agent.domain.PermissionGateway
import com.agentdesk.app.feature.agent.domain.model.AgentEvent
import com.agentdesk.app.feature.agent.domain.model.PermissionDecision
import com.agentdesk.app.feature.agent.domain.model.RunMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AgentRunService : Service() {

    companion object {
        const val ACTION_START_RUN = "com.agentdesk.app.ACTION_START_RUN"
        const val ACTION_STOP_RUN = "com.agentdesk.app.ACTION_STOP_RUN"
        const val ACTION_CONTINUE_RUN = "com.agentdesk.app.ACTION_CONTINUE_RUN"
        const val ACTION_PERMISSION_ALLOW = "com.agentdesk.app.ACTION_PERMISSION_ALLOW"
        const val ACTION_PERMISSION_DENY = "com.agentdesk.app.ACTION_PERMISSION_DENY"

        const val EXTRA_RUN_ID = "extra_run_id"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_USER_PROMPT = "extra_user_prompt"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }

    @Inject
    lateinit var agentRunner: AgentRunner

    @Inject
    lateinit var permissionGateway: PermissionGateway

    @Inject
    lateinit var notificationFactory: AgentNotificationFactory

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "agentdesk:agent_run").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RUN -> {
                val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return START_NOT_STICKY
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
                val prompt = intent.getStringExtra(EXTRA_USER_PROMPT) ?: ""
                val modeStr = intent.getStringExtra(EXTRA_MODE) ?: RunMode.ASK.name
                val mode = runCatching { RunMode.valueOf(modeStr) }.getOrDefault(RunMode.ASK)

                startRun(conversationId, projectId, prompt, mode)
            }
            ACTION_STOP_RUN -> {
                val runId = intent.getStringExtra(EXTRA_RUN_ID)
                if (runId != null) {
                    serviceScope.launch {
                        agentRunner.stop(runId)
                    }
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CONTINUE_RUN -> {
                val runId = intent.getStringExtra(EXTRA_RUN_ID) ?: return START_NOT_STICKY
                continueRun(runId)
            }
            ACTION_PERMISSION_ALLOW -> {
                val reqId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (reqId != null) {
                    serviceScope.launch {
                        permissionGateway.resolve(reqId, PermissionDecision.AllowOnce)
                    }
                }
            }
            ACTION_PERMISSION_DENY -> {
                val reqId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (reqId != null) {
                    serviceScope.launch {
                        permissionGateway.resolve(reqId, PermissionDecision.Deny("Denied by user"))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRun(conversationId: String, projectId: String, prompt: String, mode: RunMode) {
        activeJob?.cancel()
        wakeLock?.acquire(600_000L) // 10 minutes max wake lock

        val initialNotif = notificationFactory.createRunningNotification(0, "Starting agent run...", "")
        startForeground(AgentNotificationFactory.NOTIFICATION_ID, initialNotif)

        activeJob = serviceScope.launch {
            try {
                var currentStep = 0
                agentRunner.run(conversationId, projectId, prompt, mode).collect { event ->
                    when (event) {
                        is AgentEvent.StepStarted -> {
                            currentStep = event.stepIndex
                            updateNotification(currentStep, "Executing step $currentStep...")
                        }
                        is AgentEvent.ToolStarted -> {
                            updateNotification(currentStep, "Running ${event.toolName}...")
                        }
                        is AgentEvent.Completed, is AgentEvent.Cancelled, is AgentEvent.Error -> {
                            // Run finished
                        }
                        else -> Unit
                    }
                }
            } finally {
                wakeLock?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun continueRun(runId: String) {
        activeJob?.cancel()
        wakeLock?.acquire(600_000L)

        val initialNotif = notificationFactory.createRunningNotification(0, "Resuming agent run...", runId)
        startForeground(AgentNotificationFactory.NOTIFICATION_ID, initialNotif)

        activeJob = serviceScope.launch {
            try {
                agentRunner.continueRun(runId).collect { event ->
                    when (event) {
                        is AgentEvent.StepStarted -> {
                            updateNotification(event.stepIndex, "Executing step ${event.stepIndex}...")
                        }
                        is AgentEvent.ToolStarted -> {
                            updateNotification(0, "Running ${event.toolName}...")
                        }
                        else -> Unit
                    }
                }
            } finally {
                wakeLock?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateNotification(step: Int, text: String) {
        try {
            val notif = notificationFactory.createRunningNotification(step, text, "")
            NotificationManagerCompat.from(this).notify(AgentNotificationFactory.NOTIFICATION_ID, notif)
        } catch (e: SecurityException) {
            // Permission notification omitted on newer platforms if not granted
        }
    }

    override fun onDestroy() {
        activeJob?.cancel()
        wakeLock?.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
