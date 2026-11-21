package com.agentdesk.app.feature.agent.data.prompt

import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentFacts @Inject constructor(
    private val toolStateRepository: ToolStateRepository
) {
    suspend fun getEnvironmentSummary(): String {
        val states = try {
            toolStateRepository.getAllStates()
        } catch (e: Exception) {
            emptyList()
        }
        val installed = states.filter { it.status == ToolInstallStatus.INSTALLED }
        return buildString {
            append("## Environment Facts\n")
            append("- Platform: Android (arm64-v8a)\n")
            append("- Host Shell: proot Ubuntu rootfs\n")
            append("- Installed Tools:\n")
            if (installed.isEmpty()) {
                append("  - None recorded\n")
            } else {
                for (tool in installed) {
                    append("  - ${tool.toolId} (version: ${tool.installedVersion ?: "default"})\n")
                }
            }
        }
    }
}
