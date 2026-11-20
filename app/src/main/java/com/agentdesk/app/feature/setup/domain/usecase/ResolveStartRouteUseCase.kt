package com.agentdesk.app.feature.setup.domain.usecase

import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.install.DoneMarkers
import com.agentdesk.app.feature.setup.data.local.ToolStateReconciler
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.terminal.domain.BootstrapGate
import com.agentdesk.app.feature.terminal.domain.EnvironmentStatus
import javax.inject.Inject

class ResolveStartRouteUseCase @Inject constructor(
    private val bootstrapGate: BootstrapGate,
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val doneMarkers: DoneMarkers,
    private val reconciler: ToolStateReconciler
) {
    suspend operator fun invoke(): Routes {
        // Step 1: Ensure bootstrap directories
        val bootstrapResult = bootstrapGate.ensureBootstrap()
        if (bootstrapResult !is AppResult.Success) {
            return Routes.ToolSetup
        }

        // Step 2: Load catalog
        val catalogResult = catalogRepository.loadCatalog()
        if (catalogResult !is AppResult.Success) {
            return Routes.ToolSetup
        }

        // Step 3: Reconcile filesystem markers with room cache
        reconciler.reconcile()

        val tools = catalogResult.data
        val requiredTools = tools.filter { it.required }
        val states = stateRepository.getAllStates().associateBy { it.toolId }

        val allRequiredInstalled = requiredTools.all { tool ->
            val state = states[tool.id]
            val hasMarker = doneMarkers.isDone(tool.id, tool.version)
            hasMarker && state?.status == ToolInstallStatus.INSTALLED
        }

        return if (allRequiredInstalled && requiredTools.isNotEmpty()) {
            Routes.Home
        } else {
            Routes.ToolSetup
        }
    }
}
