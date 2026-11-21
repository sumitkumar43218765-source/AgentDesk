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
class WriteFileTool @Inject constructor(
    private val projectFileSystem: ProjectFileSystem
) : AgentTool {

    override val spec = ToolSpec(
        name = "write_file",
        description = "Write entire content to a file. Creates parent directories automatically. Subject to user approval.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "path": { "type": "string", "description": "Relative file path" },
                "content": { "type": "string", "description": "Full file content to write" }
              },
              "required": ["path", "content"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.ASK_ONCE

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val relPath = args["path"]?.jsonPrimitive?.content ?: ""
        val content = args["content"]?.jsonPrimitive?.content ?: ""

        return when (val result = projectFileSystem.write(ctx.projectId, relPath, content)) {
            is AppResult.Success -> {
                ToolOutput(
                    text = "<exit_code>0</exit_code>\n<output>\nFile written successfully: $relPath (${content.length} characters)\n</output>",
                    isError = false
                )
            }
            is AppResult.Failure -> {
                ToolOutput(
                    text = "<exit_code>1</exit_code>\n<output>\nWrite failed: ${result.error.message}\n</output>",
                    isError = true
                )
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val path = args["path"]?.jsonPrimitive?.content ?: ""
        val content = args["content"]?.jsonPrimitive?.content ?: ""
        val preview = content.take(300)
        return ToolSummary(
            title = "Write file: '$path'",
            risk = RiskLevel.MEDIUM,
            details = "Writing ${content.length} bytes to $path",
            diff = "+ $preview"
        )
    }
}
