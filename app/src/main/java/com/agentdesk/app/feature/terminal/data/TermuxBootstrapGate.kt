package com.agentdesk.app.feature.terminal.data

import android.content.Context
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.terminal.domain.BootstrapGate
import com.agentdesk.app.feature.terminal.domain.EnvironmentStatus
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.termux.TermuxInstaller
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermuxBootstrapGate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pathProvider: PathProvider
) : BootstrapGate {

    private val _status = MutableStateFlow<EnvironmentStatus>(EnvironmentStatus.NotInstalled)
    override val status: StateFlow<EnvironmentStatus> = _status.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        val bashExists = File(pathProvider.termuxPrefix, "bin/bash").exists() ||
                File(pathProvider.termuxPrefix, "bin/sh").exists()
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

        _status.value = EnvironmentStatus.Extracting(0.1f)
        pathProvider.termuxPrefix.mkdirs()
        pathProvider.rootfsDir.mkdirs()
        pathProvider.projectsRoot.mkdirs()
        pathProvider.tmpDir.mkdirs()

        // Check for bundled bootstrap zip asset
        val stagingDir = File(context.filesDir, "usr-staging")
        val assetBootstrap = try {
            context.assets.open("bootstrap-aarch64.zip")
        } catch (ignored: Exception) {
            null
        }

        if (assetBootstrap != null) {
            _status.value = EnvironmentStatus.Extracting(0.3f)
            val result = TermuxInstaller.setupBootstrapFromStream(
                zipInputStream = assetBootstrap,
                prefixDir = pathProvider.termuxPrefix,
                stagingDir = stagingDir,
                onProgress = { pct ->
                    _status.value = EnvironmentStatus.Extracting(0.3f + pct * 0.7f)
                }
            )

            if (result is AppResult.Failure) {
                _status.value = EnvironmentStatus.Failed(result.error)
                return result
            }
        } else {
            // Environment directories prepared; will use native Android shell or proot
            _status.value = EnvironmentStatus.Extracting(1.0f)
        }

        _status.value = EnvironmentStatus.BootstrapReady
        return AppResult.Success(Unit)
    }
}
