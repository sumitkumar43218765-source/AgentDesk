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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditFileTool @Inject constructor(
    private val projectFileSystem: ProjectFileSystem
) : AgentTool {

    override val spec = ToolSpec(
        name = "edit_file",
        description = "Perform targeted string replacement in an existing file. Exact match required.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "path": { "type": "string", "description": "Relative file path" },
                "old_str": { "type": "string", "description": "Exact text to replace" },
                "new_str": { "type": "string", "description": "Replacement text" },
                "replace_all": { "type": "boolean", "description": "Whether to replace all occurrences (default false)" }
              },
              "required": ["path", "old_str", "new_str"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.ASK_ONCE

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val relPath = args["path"]?.jsonPrimitive?.content ?: ""
        val oldStr = args["old_str"]?.jsonPrimitive?.content ?: ""
        val newStr = args["new_str"]?.jsonPrimitive?.content ?: ""
        val replaceAll = args["replace_all"]?.jsonPrimitive?.booleanOrNull ?: false

        val readRes = projectFileSystem.read(ctx.projectId, relPath)
        if (readRes is AppResult.Failure) {
            return ToolOutput(
                text = "<exit_code>1</exit_code>\n<output>\nCannot read file: ${readRes.error.message}\n</output>",
                isError = true
            )
        }

        val originalContent = (readRes as AppResult.Success).value.content
        if (!originalContent.contains(oldStr)) {
            return ToolOutput(
                text = "<exit_code>1</exit_code>\n<output>\nError: Target text not found: 'old_str' not found in $relPath. Verify file contents before editing.\n</output>",
                isError = true
            )
        }

        if (!replaceAll && originalContent.indexOf(oldStr) != originalContent.lastIndexOf(oldStr)) {
            return ToolOutput(
                text = "<exit_code>1</exit_code>\n<output>\nError: 'old_str' appears multiple times in $relPath. Provide more surrounding context or set replace_all=true.\n</output>",
                isError = true
            )
        }

        val updatedContent = if (replaceAll) {
            originalContent.replace(oldStr, newStr)
        } else {
            originalContent.replaceFirst(oldStr, newStr)
        }

        val diff = "-$oldStr\n+$newStr"
        val writeRes = projectFileSystem.write(ctx.projectId, relPath, updatedContent)
        return when (writeRes) {
            is AppResult.Success -> {
                ToolOutput(
                    text = "<exit_code>0</exit_code>\n<output>\nSuccessfully edited $relPath\n$diff\n</output>",
                    isError = false
                )
            }
            is AppResult.Failure -> {
                ToolOutput(
                    text = "<exit_code>1</exit_code>\n<output>\nFailed to write changes: ${writeRes.error.message}\n</output>",
                    isError = true
                )
            }
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val path = args["path"]?.jsonPrimitive?.content ?: ""
        val oldStr = args["old_str"]?.jsonPrimitive?.content ?: ""
        val newStr = args["new_str"]?.jsonPrimitive?.content ?: ""
        val diff = "- $oldStr\n+ $newStr"
        return ToolSummary(
            title = "Edit file: '$path'",
            risk = RiskLevel.MEDIUM,
            details = "Replacing text in $path",
            diff = diff
        )
    }
}
