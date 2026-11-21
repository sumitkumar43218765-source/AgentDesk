package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.core.common.IdGenerator
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.CommandRiskClassifier
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.build.data.parse.LogDistiller
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellExecTool @Inject constructor(
    private val shellBackend: ShellBackend,
    private val projectFileSystem: ProjectFileSystem,
    private val riskClassifier: CommandRiskClassifier
) : AgentTool {

    override val spec = ToolSpec(
        name = "shell_exec",
        description = "Execute a bash shell command in the proot Ubuntu environment within the project directory.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "command": { "type": "string", "description": "Bash command to execute" },
                "cwd": { "type": "string", "description": "Optional working directory relative to project root" },
                "timeout_sec": { "type": "integer", "description": "Timeout in seconds (default 120)" }
              },
              "required": ["command"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.ASK_ALWAYS

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val command = args["command"]?.jsonPrimitive?.content ?: ""
        if (command.isBlank()) {
            return ToolOutput(text = "Error: command cannot be blank", isError = true)
        }

        val projectGuestRoot = projectFileSystem.guestPath(ctx.projectId)
        val relCwd = args["cwd"]?.jsonPrimitive?.content
        val targetCwd = if (relCwd.isNullOrBlank()) {
            projectGuestRoot
        } else {
            if (relCwd.startsWith("/")) {
                relCwd
            } else {
                "$projectGuestRoot/$relCwd"
            }
        }

        val timeoutSec = args["timeout_sec"]?.jsonPrimitive?.longOrNull ?: 120L
        val request = ShellRequest(
            command = command,
            cwd = targetCwd,
            timeoutMs = timeoutSec * 1000L,
            executionId = IdGenerator.newId()
        )

        val result = shellBackend.runToCompletion(request)
        val combined = buildString {
            if (result.stdout.isNotEmpty()) append(result.stdout)
            if (result.stderr.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(result.stderr)
            }
        }

        val distilled = LogDistiller.distill(combined, maxChars = 6000)
        val outputText = buildString {
            append("<exit_code>${result.exitCode}</exit_code>\n")
            append("<output>\n")
            append(distilled.text)
            append("\n</output>")
            if (distilled.omittedLinesCount > 0) {
                append("\n[${distilled.omittedLinesCount} lines omitted]")
            }
        }

        return ToolOutput(
            text = outputText,
            isError = result.exitCode != 0
        )
    }

    override fun describe(args: JsonObject): ToolSummary {
        val cmd = args["command"]?.jsonPrimitive?.content ?: ""
        val risk = riskClassifier.classify(cmd)
        return ToolSummary(
            title = "Execute: $cmd",
            risk = risk,
            details = cmd
        )
    }
}
