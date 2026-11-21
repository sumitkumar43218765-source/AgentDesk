package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchFilesTool @Inject constructor(
    private val shellBackend: ShellBackend,
    private val pathProvider: PathProvider
) : AgentTool {

    override val spec = ToolSpec(
        name = "search_files",
        description = "Search for a regex pattern in files using ripgrep or grep inside the project.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "pattern": { "type": "string", "description": "Search pattern or regex" },
                "path": { "type": "string", "description": "Relative subdirectory path to search within" },
                "glob": { "type": "string", "description": "File glob filter e.g. '*.java'" }
              },
              "required": ["pattern"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val pattern = args["pattern"]?.jsonPrimitive?.content ?: ""
        val subPath = args["path"]?.jsonPrimitive?.content ?: "."
        val glob = args["glob"]?.jsonPrimitive?.content

        val globArg = if (!glob.isNullOrBlank()) "--glob '$glob'" else ""
        // Fallback to grep if rg not installed
        val command = "if command -v rg >/dev/null 2>&1; then " +
                "rg -n --max-count 50 $globArg '$pattern' '$subPath'; " +
                "else grep -rn --exclude-dir={.git,build} '$pattern' '$subPath' | head -n 50; fi"

        val request = ShellRequest(
            command = command,
            cwd = "/root/projects/${ctx.projectId}",
            environment = ShellEnvironment.PROOT_UBUNTU,
            timeoutMs = 30_000,
            executionId = UUID.randomUUID().toString()
        )

        val result = shellBackend.runToCompletion(request)
        val text = if (result.exitCode == 0 || result.stdout.isNotBlank()) {
            "<exit_code>0</exit_code>\n<output>\n${result.stdout.ifBlank { "No matches found." }}\n</output>"
        } else {
            "<exit_code>${result.exitCode}</exit_code>\n<output>\n${result.stderr.ifBlank { "No matches found." }}\n</output>"
        }

        return ToolOutput(text = text, isError = result.exitCode > 1)
    }

    override fun describe(args: JsonObject): ToolSummary {
        val pattern = args["pattern"]?.jsonPrimitive?.content ?: ""
        return ToolSummary(
            title = "Search files for: '$pattern'",
            risk = RiskLevel.LOW
        )
    }
}
