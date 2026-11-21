package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeletePathTool @Inject constructor(
    private val projectFileSystem: ProjectFileSystem
) : AgentTool {

    override val spec = ToolSpec(
        name = "delete_path",
        description = "Delete a file or directory within the project. Always requires explicit user confirmation.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "path": { "type": "string", "description": "Relative file or directory path to delete" }
              },
              "required": ["path"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.ASK_ALWAYS

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val relPath = args["path"]?.jsonPrimitive?.content ?: ""
        if (relPath.isBlank() || relPath == "." || relPath == "/") {
            return ToolOutput(
                text = "<exit_code>1</exit_code>\n<output>\nCannot delete project root\n</output>",
                isError = true
            )
        }

        return when (val result = projectFileSystem.delete(ctx.projectId, relPath)) {
            is AppResult.Success -> {
                ToolOutput(
                    text = "<exit_code>0</exit_code>\n<output>\nDeleted: $relPath\n</output>",
                    isError = false
                )
            }
            is AppResult.Failure -> {
                ToolOutput(
                    text = "<exit_code>1</exit_code>\n<output>\nDelete failed: ${result.error.message}\n</output>",
                    isError = true
                )
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val path = args["path"]?.jsonPrimitive?.content ?: ""
        return ToolSummary(
            title = "Delete path: '$path'",
            risk = RiskLevel.HIGH,
            details = "Permanently deleting $path from project"
        )
    }
}
