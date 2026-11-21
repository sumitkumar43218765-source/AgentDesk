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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListDirTool @Inject constructor(
    private val projectFileSystem: ProjectFileSystem
) : AgentTool {

    override val spec = ToolSpec(
        name = "list_dir",
        description = "List files and directories within a path relative to the project root.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "path": { "type": "string", "description": "Relative directory path (e.g. '', 'src', 'res')" },
                "depth": { "type": "integer", "description": "Maximum directory traversal depth (default 1)" }
              },
              "required": ["path"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val relPath = args["path"]?.jsonPrimitive?.content ?: ""
        val depth = args["depth"]?.jsonPrimitive?.intOrNull ?: 1

        return when (val result = projectFileSystem.list(ctx.projectId, relPath, depth)) {
            is AppResult.Success -> {
                val lines = result.value.map { entry ->
                    val typeStr = if (entry.isDirectory) "[DIR]" else "[FILE]"
                    "$typeStr ${entry.relativePath} (${entry.sizeBytes} bytes)"
                }
                val output = if (lines.isEmpty()) "(empty directory)" else lines.joinToString("\n")
                ToolOutput(text = "<exit_code>0</exit_code>\n<output>\n$output\n</output>", isError = false)
            }
            is AppResult.Failure -> {
                ToolOutput(text = "<exit_code>1</exit_code>\n<output>\nError: ${result.error.message}\n</output>", isError = true)
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val path = args["path"]?.jsonPrimitive?.content ?: ""
        return ToolSummary(
            title = "List directory: '$path'",
            risk = RiskLevel.LOW
        )
    }
}
