package com.agentdesk.app.feature.setup.data.install

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.os.PowerManager
import com.agentdesk.app.core.logging.AppLogger
import com.agentdesk.app.core.common.DispatcherProvider
import com.agentdesk.app.feature.setup.domain.model.Phase
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SetupInstallService : Service() {

    @Inject
    lateinit var notificationFactory: SetupNotificationFactory

    @Inject
    lateinit var shellBackend: ShellBackend

    @Inject
    lateinit var pathProvider: PathProvider

    @Inject
    lateinit var statusTailer: StatusTailer

    @Inject
    lateinit var toolStateRepository: ToolStateRepository

    @Inject
    lateinit var dispatchers: DispatcherProvider

    @Inject
    lateinit var logger: AppLogger

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPaused = false

    companion object {
        const val ACTION_START = "com.agentdesk.app.ACTION_START_INSTALL"
        private val _currentPhase = MutableStateFlow(Phase.PLANNING)
        val currentPhase = _currentPhase.asStateFlow()
        private val _overallPct = MutableStateFlow(0)
        val overallPct = _overallPct.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AgentDesk:SetupInstallWakeLock"
        )
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == SetupNotificationFactory.ACTION_CANCEL) {
            cancelInstallation()
            return START_NOT_STICKY
        }

        startForeground(
            SetupNotificationFactory.NOTIFICATION_ID,
            notificationFactory.buildNotification(
                title = "Installing tools",
                message = "Preparing environment...",
                progressPct = 0
            )
        )

        serviceScope.launch(dispatchers.io) {
            runInstallation()
        }

        return START_STICKY
    }

    private suspend fun runInstallation() {
        wakeLock?.acquire(30 * 60 * 1000L) // 30 min max
        _currentPhase.value = Phase.INSTALLING

        // Start observing status.jsonl
        val tailJob = serviceScope.launch(dispatchers.io) {
            statusTailer.tailEvents().collect { event ->
                handleStatusEvent(event.tool, event.event, event.pct, event.msg)
            }
        }

        try {
            val scriptFile = File(pathProvider.setupStateDir, "run_install.sh")
            if (!scriptFile.exists()) {
                logger.error("Install script not found: ${scriptFile.absolutePath}")
                _currentPhase.value = Phase.FAILED
                stopSelf()
                return
            }

            scriptFile.setExecutable(true, false)

            val request = ShellRequest(
                id = "setup_installer",
                command = "bash \"${scriptFile.absolutePath}\"",
                environment = ShellEnvironment.TERMUX_NATIVE,
                workingDir = pathProvider.termuxHome.absolutePath,
                envVars = mapOf(
                    "AGENTDESK_HOME" to pathProvider.termuxHome.absolutePath,
                    "AGENTDESK_ROOTFS" to pathProvider.rootfsDir.absolutePath
                )
            )

            val result = shellBackend.execute(request)
            if (result.exitCode == 0) {
                _currentPhase.value = Phase.DONE
                _overallPct.value = 100
                updateNotification("Installation Complete", "All tools configured", 100)
            } else {
                _currentPhase.value = Phase.FAILED
                updateNotification("Installation Failed", "Exit code: ${result.exitCode}", 0)
            }
        } catch (e: Exception) {
            logger.error("Exception during installation: ${e.message}", e)
            _currentPhase.value = Phase.FAILED
        } finally {
            tailJob.cancel()
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private suspend fun handleStatusEvent(toolId: String, event: String, pct: Int, msg: String) {
        val status = when (event) {
            "start" -> ToolInstallStatus.INSTALLING
            "ok" -> ToolInstallStatus.INSTALLED
            "fail" -> ToolInstallStatus.FAILED
            "skip" -> ToolInstallStatus.INSTALLED
            else -> ToolInstallStatus.INSTALLING
        }

        toolStateRepository.setToolStatus(
            toolId = toolId,
            status = status,
            error = if (event == "fail") msg else null
        )

        updateNotification(
            title = "Installing $toolId",
            message = msg.ifEmpty { "Progress: $pct%" },
            progressPct = pct
        )
    }

    private fun updateNotification(title: String, message: String, progressPct: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(
            SetupNotificationFactory.NOTIFICATION_ID,
            notificationFactory.buildNotification(title, message, progressPct)
        )
    }

    private fun cancelInstallation() {
        logger.info("Installation cancelled by user")
        serviceScope.cancel()
        _currentPhase.value = Phase.FAILED
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        cm.registerNetworkCallback(
            builder.build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    isPaused = true
                    _currentPhase.value = Phase.PAUSED
                    updateNotification("Paused", "Waiting for network connection...", -1)
                }

                override fun onAvailable(network: Network) {
                    if (isPaused) {
                        isPaused = false
                        _currentPhase.value = Phase.INSTALLING
                        updateNotification("Resuming", "Network reconnected", -1)
                    }
                }
            }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
