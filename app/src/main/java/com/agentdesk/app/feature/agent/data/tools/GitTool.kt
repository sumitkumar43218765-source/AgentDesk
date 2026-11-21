package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import com.agentdesk.app.feature.projects.domain.repository.GitService
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitTool @Inject constructor(
    private val gitService: GitService
) : AgentTool {

    override val spec = ToolSpec(
        name = "git",
        description = "Execute git operations on the current project repository.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "subcommand": { 
                  "type": "string", 
                  "enum": ["status", "diff", "log", "commit", "push", "restore"],
                  "description": "Git subcommand to execute"
                },
                "args": { 
                  "type": "string", 
                  "description": "Optional argument (e.g. commit message for 'commit', commit SHA for 'restore')" 
                }
              },
              "required": ["subcommand"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val subcommand = args["subcommand"]?.jsonPrimitive?.content ?: ""
        val extra = args["args"]?.jsonPrimitive?.content ?: ""

        return when (subcommand) {
            "status" -> {
                when (val res = gitService.status(ctx.projectId)) {
                    is AppResult.Success -> {
                        val st = res.value
                        val text = "Branch: ${st.branch}\nClean: ${st.clean}\nStaged: ${st.stagedCount}\nUnstaged: ${st.unstagedCount}\nUntracked: ${st.untrackedCount}"
                        ToolOutput(text = text, isError = false)
                    }
                    is AppResult.Failure -> ToolOutput(text = "Git status error: ${res.error.message}", isError = true)
                }
            }
            "diff" -> {
                when (val res = gitService.diffStat(ctx.projectId, extra.ifBlank { null })) {
                    is AppResult.Success -> ToolOutput(text = res.value.ifBlank { "No changes." }, isError = false)
                    is AppResult.Failure -> ToolOutput(text = "Git diff error: ${res.error.message}", isError = true)
                }
            }
            "log" -> {
                when (val res = gitService.log(ctx.projectId, limit = 10)) {
                    is AppResult.Success -> {
                        val logText = res.value.joinToString("\n") { "${it.hash.take(7)} - ${it.message} (${it.author})" }
                        ToolOutput(text = logText.ifBlank { "No commits yet." }, isError = false)
                    }
                    is AppResult.Failure -> ToolOutput(text = "Git log error: ${res.error.message}", isError = true)
                }
            }
            "commit", "add" -> {
                val message = extra.ifBlank { "Agent checkpoint" }
                when (val res = gitService.checkpoint(ctx.projectId, message)) {
                    is AppResult.Success -> {
                        val commit = res.value
                        val text = if (commit != null) {
                            "Committed: ${commit.hash.take(7)} ${commit.message}"
                        } else {
                            "Nothing to commit (working tree clean)."
                        }
                        ToolOutput(text = text, isError = false)
                    }
                    is AppResult.Failure -> ToolOutput(text = "Git commit error: ${res.error.message}", isError = true)
                }
            }
            "push" -> {
                when (val res = gitService.push(ctx.projectId)) {
                    is AppResult.Success -> ToolOutput(text = "Pushed changes to remote repository.", isError = false)
                    is AppResult.Failure -> ToolOutput(text = "Git push error: ${res.error.message}", isError = true)
                }
            }
            "restore" -> {
                if (extra.isBlank()) {
                    return ToolOutput(text = "Error: commit SHA required for restore", isError = true)
                }
                when (val res = gitService.restore(ctx.projectId, extra)) {
                    is AppResult.Success -> ToolOutput(text = "Restored project to commit: $extra", isError = false)
                    is AppResult.Failure -> ToolOutput(text = "Git restore error: ${res.error.message}", isError = true)
                }
            }
            else -> ToolOutput(text = "Unsupported git subcommand: $subcommand", isError = true)
        }
    }

    override fun describe(args: JsonObject): ToolSummary {
        val sub = args["subcommand"]?.jsonPrimitive?.content ?: "git"
        val extra = args["args"]?.jsonPrimitive?.content ?: ""
        val risk = if (sub in listOf("push", "restore")) RiskLevel.HIGH else RiskLevel.LOW
        return ToolSummary(
            title = "Git: $sub ${extra.take(20)}".trim(),
            risk = risk,
            details = "git $sub $extra"
        )
    }
}
