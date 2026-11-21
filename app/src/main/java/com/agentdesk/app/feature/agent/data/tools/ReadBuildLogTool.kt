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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadBuildLogTool @Inject constructor(
    private val buildEngine: BuildEngine
) : AgentTool {

    override val spec = ToolSpec(
        name = "read_build_log",
        description = "Read a paginated slice of the build log for a specific build run.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "build_id": { "type": "string", "description": "The build run ID" },
                "offset": { "type": "integer", "description": "Zero-based starting line offset (default 0)" },
                "limit": { "type": "integer", "description": "Maximum number of lines to read (default 100)" }
              },
              "required": ["build_id"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val buildId = args["build_id"]?.jsonPrimitive?.content ?: ""
        if (buildId.isBlank()) {
            return ToolOutput(text = "Error: build_id is required", isError = true)
        }
        val offset = args["offset"]?.jsonPrimitive?.intOrNull ?: 0
        val limit = args["limit"]?.jsonPrimitive?.intOrNull ?: 100

        return when (val result = buildEngine.readLog(buildId, offset, limit)) {
            is AppResult.Success -> {
                val slice = result.value
                val body = buildString {
                    append("<build_id>$buildId</build_id>\n")
                    append("<log_slice offset=\"${slice.offset}\" count=\"${slice.lines.size}\" total=\"${slice.totalLines}\">\n")
                    append(slice.lines.joinToString("\n"))
                    append("\n</log_slice>")
                }
                ToolOutput(text = body, isError = false)
            }
            is AppResult.Failure -> {
                ToolOutput(text = "Failed to read build log: ${result.error.message}", isError = true)
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val buildId = args["build_id"]?.jsonPrimitive?.content ?: ""
        return ToolSummary(
            title = "Read Build Log: $buildId",
            risk = RiskLevel.LOW
        )
    }
}
