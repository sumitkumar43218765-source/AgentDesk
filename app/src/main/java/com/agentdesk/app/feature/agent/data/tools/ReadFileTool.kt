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
class ReadFileTool @Inject constructor(
    private val projectFileSystem: ProjectFileSystem
) : AgentTool {

    override val spec = ToolSpec(
        name = "read_file",
        description = "Read lines of a file relative to the project root with line numbers (max 256KB).",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "path": { "type": "string", "description": "Relative file path" },
                "start_line": { "type": "integer", "description": "1-based starting line number" },
                "end_line": { "type": "integer", "description": "1-based ending line number" }
              },
              "required": ["path"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val relPath = args["path"]?.jsonPrimitive?.content ?: ""
        val startLine = args["start_line"]?.jsonPrimitive?.intOrNull
        val endLine = args["end_line"]?.jsonPrimitive?.intOrNull

        return when (val result = projectFileSystem.read(ctx.projectId, relPath)) {
            is AppResult.Success -> {
                val lines = result.value.content.lines()
                val totalLines = lines.size
                val start = ((startLine ?: 1) - 1).coerceIn(0, totalLines)
                val end = (endLine ?: totalLines).coerceIn(start, totalLines)

                val numbered = lines.subList(start, end).mapIndexed { idx, line ->
                    "${start + idx + 1}: $line"
                }.joinToString("\n")

                ToolOutput(
                    text = "<exit_code>0</exit_code>\n<output>\n$numbered\n</output>",
                    isError = false
                )
            }
            is AppResult.Failure -> {
                ToolOutput(
                    text = "<exit_code>1</exit_code>\n<output>\nError: ${result.error.message}\n</output>",
                    isError = true
                )
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val path = args["path"]?.jsonPrimitive?.content ?: ""
        return ToolSummary(
            title = "Read file: '$path'",
            risk = RiskLevel.LOW
        )
    }
}
