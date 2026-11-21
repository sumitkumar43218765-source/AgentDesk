package com.agentdesk.app.feature.build.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.agentdesk.app.feature.build.data.engine.BuildScheduler
import com.agentdesk.app.feature.build.data.engine.DefaultBuildEngine
import com.agentdesk.app.feature.build.domain.model.BuildEvent
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BuildService : Service() {

    companion object {
        const val ACTION_START_BUILD = "com.agentdesk.app.ACTION_START_BUILD"
        const val ACTION_CANCEL_BUILD = "com.agentdesk.app.ACTION_CANCEL_BUILD"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_VARIANT = "extra_variant"
        const val EXTRA_BUILD_ID = "extra_build_id"
    }

    @Inject
    lateinit var buildEngine: DefaultBuildEngine

    @Inject
    lateinit var buildScheduler: BuildScheduler

    @Inject
    lateinit var notificationFactory: BuildNotificationFactory

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "agentdesk:build").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BUILD -> {
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
                val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Project"
                val variantStr = intent.getStringExtra(EXTRA_VARIANT) ?: BuildVariant.DEBUG.name
                val variant = runCatching { BuildVariant.valueOf(variantStr) }.getOrDefault(BuildVariant.DEBUG)

                startBuild(projectId, projectName, variant)
            }
            ACTION_CANCEL_BUILD -> {
                val buildId = intent.getStringExtra(EXTRA_BUILD_ID)
                if (buildId != null) {
                    serviceScope.launch {
                        buildEngine.cancel(buildId)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startBuild(projectId: String, projectName: String, variant: BuildVariant) {
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max timeout

        val initialNotif = notificationFactory.createNotification(
            projectName = projectName,
            currentStep = null,
            progressIndex = 0,
            totalSteps = 7,
            buildId = ""
        )
        startForeground(BuildNotificationFactory.NOTIFICATION_ID, initialNotif)

        activeJob?.cancel()
        activeJob = serviceScope.launch {
            var activeBuildId = ""
            var stepIndex = 0

            buildEngine.build(projectId, variant).collect { event ->
                when (event) {
                    is BuildEvent.Started -> {
                        activeBuildId = event.buildId
                    }
                    is BuildEvent.StepStarted -> {
                        stepIndex++
                        val notif = notificationFactory.createNotification(
                            projectName = projectName,
                            currentStep = event.step,
                            progressIndex = stepIndex,
                            totalSteps = 7,
                            buildId = activeBuildId
                        )
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        manager.notify(BuildNotificationFactory.NOTIFICATION_ID, notif)
                    }
                    is BuildEvent.Finished -> {
                        stopSelf()
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
