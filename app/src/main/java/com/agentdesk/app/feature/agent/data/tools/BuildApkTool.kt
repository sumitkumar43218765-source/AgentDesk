package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.ArtifactRef
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.build.data.parse.LogDistiller
import com.agentdesk.app.feature.build.domain.BuildEngine
import com.agentdesk.app.feature.build.domain.model.BuildEvent
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildStatus
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildApkTool @Inject constructor(
    private val buildEngine: BuildEngine
) : AgentTool {

    override val spec = ToolSpec(
        name = "build_apk",
        description = "Compile, link, dex, align, and sign an APK for the current project.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "variant": { "type": "string", "enum": ["debug", "release"], "description": "Build variant (default debug)" }
              }
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val variantStr = args["variant"]?.jsonPrimitive?.content?.lowercase() ?: "debug"
        val variant = if (variantStr == "release") BuildVariant.RELEASE else BuildVariant.DEBUG

        val finalRun: BuildRun = try {
            val event = buildEngine.build(ctx.projectId, variant)
                .filterIsInstance<BuildEvent.Finished>()
                .first()
            event.run
        } catch (e: Exception) {
            return ToolOutput(
                text = "<build_id>unknown</build_id>\n<status>FAILED</status>\nBuild execution error: ${e.message}",
                isError = true
            )
        }

        return if (finalRun.status == BuildStatus.SUCCESS) {
            val apkPath = finalRun.apkPath ?: ""
            val artifact = ArtifactRef(
                id = finalRun.id,
                type = "apk",
                path = apkPath
            )
            ToolOutput(
                text = """
                    <build_id>${finalRun.id}</build_id>
                    <status>SUCCESS</status>
                    APK built successfully: $apkPath
                    Duration: ${finalRun.durationMs}ms
                """.trimIndent(),
                isError = false,
                artifacts = listOf(artifact)
            )
        } else {
            val rawError = finalRun.distilledSummary ?: "Build failed with exit code ${finalRun.exitCode}"
            val distilled = LogDistiller.distill(rawError, maxChars = 6000)
            ToolOutput(
                text = """
                    <build_id>${finalRun.id}</build_id>
                    <status>FAILED</status>
                    <errors>
                    ${distilled.text}
                    </errors>
                """.trimIndent(),
                isError = true
            )
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val variant = args["variant"]?.jsonPrimitive?.content ?: "debug"
        return ToolSummary(
            title = "Build APK ($variant)",
            risk = RiskLevel.LOW
        )
    }
}
