package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.model.PermissionLevel
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import com.agentdesk.app.feature.agent.domain.model.ToolContext
import com.agentdesk.app.feature.agent.domain.model.ToolOutput
import com.agentdesk.app.feature.agent.domain.model.ToolSummary
import com.agentdesk.app.feature.agent.domain.usecase.AnswerAskUserUseCase
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AskUserTool @Inject constructor(
    private val answerAskUserUseCase: AnswerAskUserUseCase
) : AgentTool {

    override val spec = ToolSpec(
        name = "ask_user",
        description = "Ask the user a clarifying question and wait for their answer.",
        inputSchemaJson = """
            {
              "type": "object",
              "properties": {
                "question": { "type": "string", "description": "The question to ask the user" },
                "options": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Optional list of predefined choices"
                }
              },
              "required": ["question"]
            }
        """.trimIndent()
    )

    override val permission = PermissionLevel.AUTO

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput {
        val question = args["question"]?.jsonPrimitive?.content ?: ""
        if (question.isBlank()) {
            return ToolOutput(text = "Error: question cannot be blank", isError = true)
        }

        val questionId = ctx.toolCallId.ifBlank { ctx.runId }
        val userAnswer = answerAskUserUseCase.answers.first { answer ->
            answer.questionId == questionId || answer.questionId == ctx.runId
        }

        return ToolOutput(
            text = userAnswer.answer,
            isError = false
        )
    }

    override fun describe(args: JsonObject): ToolSummary {
        val question = args["question"]?.jsonPrimitive?.content ?: ""
        return ToolSummary(
            title = "Ask user: ${question.take(40)}",
            risk = RiskLevel.LOW,
            details = question
        )
    }
}
