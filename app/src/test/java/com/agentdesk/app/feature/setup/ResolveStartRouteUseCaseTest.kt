package com.agentdesk.app.feature.setup

import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.install.DoneMarkers
import com.agentdesk.app.feature.setup.data.local.ToolStateReconciler
import com.agentdesk.app.feature.setup.domain.model.ToolCategory
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.model.ToolSource
import com.agentdesk.app.feature.setup.domain.model.ToolSourceKind
import com.agentdesk.app.feature.setup.domain.model.ToolState
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.setup.domain.usecase.ResolveStartRouteUseCase
import com.agentdesk.app.feature.terminal.domain.BootstrapGate
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ResolveStartRouteUseCaseTest {

    private val bootstrapGate = mockk<BootstrapGate>()
    private val catalogRepository = mockk<ToolCatalogRepository>()
    private val stateRepository = mockk<ToolStateRepository>()
    private val doneMarkers = mockk<DoneMarkers>()
    private val reconciler = mockk<ToolStateReconciler>(relaxed = true)

    private lateinit var useCase: ResolveStartRouteUseCase

    private val requiredTool = ToolDefinition(
        id = "rootfs-ubuntu",
        name = "Ubuntu Rootfs",
        description = "base",
        category = ToolCategory.CORE,
        required = true,
        dependsOn = emptyList(),
        sizeEstimateMb = 800L,
        environment = ShellEnvironment.TERMUX_NATIVE,
        source = ToolSource(ToolSourceKind.APT),
        install = emptyList(),
        verify = "cat /etc/os-release",
        version = "22.04"
    )

    @Before
    fun setUp() {
        useCase = ResolveStartRouteUseCase(
            bootstrapGate = bootstrapGate,
            catalogRepository = catalogRepository,
            stateRepository = stateRepository,
            doneMarkers = doneMarkers,
            reconciler = reconciler
        )
    }

    @Test
    fun invoke_whenRequiredToolsInstalledAndMarked_routesToHome() = runTest {
        coEvery { bootstrapGate.ensureBootstrap() } returns AppResult.Success(Unit)
        coEvery { catalogRepository.loadCatalog() } returns AppResult.Success(listOf(requiredTool))
        coEvery { doneMarkers.isDone("rootfs-ubuntu", "22.04") } returns true
        coEvery { stateRepository.getAllStates() } returns listOf(
            ToolState(toolId = "rootfs-ubuntu", installedVersion = "22.04", status = ToolInstallStatus.INSTALLED)
        )

        val route = useCase()
        assertEquals(Routes.Home, route)
    }

    @Test
    fun invoke_whenRequiredToolsNotDone_routesToToolSetup() = runTest {
        coEvery { bootstrapGate.ensureBootstrap() } returns AppResult.Success(Unit)
        coEvery { catalogRepository.loadCatalog() } returns AppResult.Success(listOf(requiredTool))
        coEvery { doneMarkers.isDone("rootfs-ubuntu", "22.04") } returns false
        coEvery { stateRepository.getAllStates() } returns emptyList()

        val route = useCase()
        assertEquals(Routes.ToolSetup, route)
    }
}
