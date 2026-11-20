package com.agentdesk.app.feature.setup

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.InstallPlanner
import com.agentdesk.app.feature.setup.domain.model.ToolCategory
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.model.ToolSource
import com.agentdesk.app.feature.setup.domain.model.ToolSourceKind
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InstallPlannerTest {

    private lateinit var planner: InstallPlanner

    private fun createTool(
        id: String,
        required: Boolean = false,
        dependsOn: List<String> = emptyList(),
        sizeMb: Long = 100L
    ): ToolDefinition {
        return ToolDefinition(
            id = id,
            name = id,
            description = "desc",
            category = ToolCategory.CORE,
            required = required,
            dependsOn = dependsOn,
            sizeEstimateMb = sizeMb,
            environment = ShellEnvironment.PROOT_UBUNTU,
            source = ToolSource(ToolSourceKind.APT),
            install = emptyList(),
            verify = "true",
            version = "1.0"
        )
    }

    @Before
    fun setUp() {
        planner = InstallPlanner()
    }

    @Test
    fun plan_resolvesTopologicalOrder() {
        val rootfs = createTool("rootfs-ubuntu", required = true)
        val clang = createTool("ndk-clang", required = false, dependsOn = listOf("rootfs-ubuntu"), sizeMb = 500)
        val cmake = createTool("cmake-ninja", required = false, dependsOn = listOf("ndk-clang"), sizeMb = 80)

        val result = planner.plan(
            allTools = listOf(rootfs, clang, cmake),
            selectedToolIds = setOf("cmake-ninja"),
            availableDiskSpaceMb = 5000L
        )

        assertTrue(result is AppResult.Success)
        val plan = (result as AppResult.Success).data
        val ids = plan.toolsToInstall.map { it.id }

        // Must install rootfs before clang before cmake
        val rootfsIdx = ids.indexOf("rootfs-ubuntu")
        val clangIdx = ids.indexOf("ndk-clang")
        val cmakeIdx = ids.indexOf("cmake-ninja")

        assertTrue(rootfsIdx >= 0)
        assertTrue(clangIdx > rootfsIdx)
        assertTrue(cmakeIdx > clangIdx)
    }

    @Test
    fun plan_detectsCircularDependency() {
        val toolA = createTool("tool-a", dependsOn = listOf("tool-b"))
        val toolB = createTool("tool-b", dependsOn = listOf("tool-a"))

        val result = planner.plan(
            allTools = listOf(toolA, toolB),
            selectedToolIds = setOf("tool-a"),
            availableDiskSpaceMb = 5000L
        )

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun plan_calculatesSpaceCheckCorrectly() {
        val tool = createTool("tool-a", sizeMb = 1000L)
        // required space = 1000 * 1.5 + 500 = 2000 MB

        val resultEnough = planner.plan(
            allTools = listOf(tool),
            selectedToolIds = setOf("tool-a"),
            availableDiskSpaceMb = 3000L
        )
        assertTrue(resultEnough is AppResult.Success)
        val planEnough = (resultEnough as AppResult.Success).data
        assertTrue(planEnough.hasEnoughSpace)
        assertEquals(0L, planEnough.spaceShortfallMb)

        val resultShortfall = planner.plan(
            allTools = listOf(tool),
            selectedToolIds = setOf("tool-a"),
            availableDiskSpaceMb = 1500L
        )
        assertTrue(resultShortfall is AppResult.Success)
        val planShortfall = (resultShortfall as AppResult.Success).data
        assertFalse(planShortfall.hasEnoughSpace)
        assertEquals(500L, planShortfall.spaceShortfallMb)
    }
}
