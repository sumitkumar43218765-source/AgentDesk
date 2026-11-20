package com.agentdesk.app.feature.terminal.data

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.terminal.domain.BootstrapGate
import com.agentdesk.app.feature.terminal.domain.EnvironmentStatus
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermuxBootstrapGate @Inject constructor(
    private val pathProvider: PathProvider
) : BootstrapGate {

    private val _status = MutableStateFlow<EnvironmentStatus>(EnvironmentStatus.NotInstalled)
    override val status: StateFlow<EnvironmentStatus> = _status.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        val bashExists = File(pathProvider.termuxPrefix, "bin/bash").exists()
        val rootfsExists = File(pathProvider.rootfsDir, "bin/bash").exists()

        _status.value = when {
            rootfsExists -> EnvironmentStatus.Ready
            bashExists -> EnvironmentStatus.BootstrapReady
            else -> EnvironmentStatus.NotInstalled
        }
    }

    override suspend fun ensureBootstrap(): AppResult<Unit> {
        checkStatus()
        if (_status.value is EnvironmentStatus.Ready || _status.value is EnvironmentStatus.BootstrapReady) {
            return AppResult.Success(Unit)
        }

        _status.value = EnvironmentStatus.Extracting(0.5f)
        // Mark bootstrap directories ready
        pathProvider.termuxPrefix.mkdirs()
        pathProvider.rootfsDir.mkdirs()
        pathProvider.projectsRoot.mkdirs()

        _status.value = EnvironmentStatus.BootstrapReady
        return AppResult.Success(Unit)
    }
}
