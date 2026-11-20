package com.agentdesk.app.feature.setup.data.local

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.install.DoneMarkers
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolState
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolStateReconciler @Inject constructor(
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val doneMarkers: DoneMarkers,
    private val shellBackend: ShellBackend,
    private val pathProvider: PathProvider
) {

    suspend fun reconcile() {
        val catalogResult = catalogRepository.loadCatalog()
        if (catalogResult !is AppResult.Success) return

        val tools = catalogResult.value
        val dbStates = stateRepository.getAllStates().associateBy { it.toolId }

        for (tool in tools) {
            val isDone = doneMarkers.isDone(tool.id, tool.version)
            val currentState = dbStates[tool.id]

            if (isDone) {
                val request = ShellRequest(
                    command = tool.verify,
                    cwd = if (tool.environment == ShellEnvironment.TERMUX_NATIVE) {
                        pathProvider.termuxHome.absolutePath
                    } else {
                        pathProvider.rootfsHome.absolutePath
                    },
                    environment = tool.environment,
                    executionId = "reconcile_${tool.id}"
                )
                val verifyResult = shellBackend.runToCompletion(request)
                if (verifyResult.exitCode == 0) {
                    stateRepository.updateToolState(
                        ToolState(
                            toolId = tool.id,
                            installedVersion = tool.version,
                            status = ToolInstallStatus.INSTALLED,
                            lastVerifiedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    stateRepository.updateToolState(
                        ToolState(
                            toolId = tool.id,
                            installedVersion = tool.version,
                            status = ToolInstallStatus.FAILED,
                            lastError = "Verification check failed during reconciliation"
                        )
                    )
                }
            } else if (currentState == null) {
                stateRepository.updateToolState(
                    ToolState(
                        toolId = tool.id,
                        status = ToolInstallStatus.NOT_INSTALLED
                    )
                )
            }
        }
    }
}
