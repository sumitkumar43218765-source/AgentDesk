package com.agentdesk.app.feature.setup.data.check

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.PostInstallCheck
import com.agentdesk.app.feature.setup.domain.model.CheckResult
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolVerifyCheck @Inject constructor(
    private val catalogRepository: ToolCatalogRepository,
    private val stateRepository: ToolStateRepository,
    private val shellBackend: ShellBackend,
    private val pathProvider: PathProvider
) : PostInstallCheck {

    override val id: String = "tool_verify_all"
    override val title: String = "Verify Installed Tool Binaries"

    override suspend fun run(): CheckResult {
        val catalog = catalogRepository.loadCatalog()
        val tools = if (catalog is AppResult.Success) {
            catalog.value
        } else {
            return CheckResult.Fail("Could not load tool catalog")
        }

        val states = stateRepository.getAllStates().associateBy { it.toolId }
        val failures = mutableListOf<String>()

        for (tool in tools) {
            val state = states[tool.id]
            val shouldVerify = tool.required || state?.status == ToolInstallStatus.INSTALLED

            if (shouldVerify) {
                val request = ShellRequest(
                    command = tool.verify,
                    cwd = if (tool.environment == ShellEnvironment.TERMUX_NATIVE) {
                        pathProvider.termuxHome.absolutePath
                    } else {
                        pathProvider.rootfsHome.absolutePath
                    },
                    environment = tool.environment,
                    executionId = "verify_${tool.id}"
                )

                val result = shellBackend.runToCompletion(request)
                if (result.exitCode != 0) {
                    failures.add("${tool.name} (${tool.id}): verification command failed (exit code ${result.exitCode})")
                }
            }
        }

        return if (failures.isEmpty()) {
            CheckResult.Pass
        } else {
            CheckResult.Fail(
                message = "${failures.size} tool verification checks failed",
                details = failures.joinToString("\n")
            )
        }
    }
}
