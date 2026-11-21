package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.model.BuildStatus
import com.agentdesk.app.feature.build.domain.usecase.InstallApkUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallApkTool @Inject constructor(
    private val buildEngine: BuildEngine,
    private val installApkUseCase: InstallApkUseCase
) : AgentTool {

    override val spec = ToolSpec(
        name = "install_apk",
        description = "Install the built APK on the user's Android device.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "build_id": { "type": "string", "description": "Optional build ID. If omitted, installs the latest successful build." }
              }
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.ASK_ALWAYS

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val buildId = args["build_id"]?.jsonPrimitive?.content

        val targetRun = if (!buildId.isNullOrBlank()) {
            buildEngine.getRun(buildId)
        } else {
            val history = buildEngine.observeHistory(ctx.projectId).firstOrNull() ?: emptyList()
            history.firstOrNull { it.status == BuildStatus.SUCCESS && !it.apkHostPath.isNullOrBlank() }
        }

        if (targetRun == null || targetRun.apkHostPath.isNullOrBlank()) {
            return ToolOutput(
                text = "No successful APK build found to install. Run build_apk first.",
                isError = true
            )
        }

        return when (val result = installApkUseCase(targetRun.apkHostPath)) {
            is AppResult.Success -> {
                ToolOutput(
                    text = "APK installation initiated: ${result.value}",
                    isError = false
                )
            }
            is AppResult.Failure -> {
                ToolOutput(
                    text = "APK installation failed: ${result.error.message}",
                    isError = true
                )
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val buildId = args["build_id"]?.jsonPrimitive?.content ?: "latest"
        return ToolSummary(
            title = "Install APK ($buildId)",
            risk = RiskLevel.HIGH,
            details = "Requests user permission to install the generated package on device."
        )
    }
}
