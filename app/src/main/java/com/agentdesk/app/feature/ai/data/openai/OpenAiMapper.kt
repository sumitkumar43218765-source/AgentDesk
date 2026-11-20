package com.agentdesk.app.feature.ai.data.openai

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.Role
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiMapper @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun toRequestBodyJson(request: LlmRequest): String {
        val messages = mutableListOf<OpenAiMessage>()

        if (!request.system.isNullOrBlank()) {
            messages.add(OpenAiMessage(role = "system", content = request.system))
        }

        for (msg in request.messages) {
            when (msg.role) {
                Role.USER -> {
                    val textBuilder = StringBuilder()
                    for (block in msg.blocks) {
                        when (block) {
                            is ContentBlock.Text -> textBuilder.append(block.text)
                            is ContentBlock.ToolResult -> {
                                messages.add(
                                    OpenAiMessage(
                                        role = "tool",
                                        content = block.content,
                                        toolCallId = block.toolUseId
                                    )
                                )
                            }
                            else -> {}
                        }
                    }
                    if (textBuilder.isNotEmpty()) {
                        messages.add(OpenAiMessage(role = "user", content = textBuilder.toString()))
                    }
                }
                Role.ASSISTANT -> {
                    var textContent: String? = null
                    val toolCalls = mutableListOf<OpenAiToolCall>()

                    for (block in msg.blocks) {
                        when (block) {
                            is ContentBlock.Text -> textContent = block.text
                            is ContentBlock.ToolUse -> {
                                toolCalls.add(
                                    OpenAiToolCall(
                                        id = block.id,
                                        function = OpenAiFunctionCall(
                                            name = block.name,
                                            arguments = block.argsJson
                                        )
                                    )
                                )
                            }
                            else -> {}
                        }
                    }

                    messages.add(
                        OpenAiMessage(
                            role = "assistant",
                            content = textContent,
                            toolCalls = if (toolCalls.isNotEmpty()) toolCalls else null
                        )
                    )
                }
            }
        }

        val tools = if (request.tools.isNotEmpty()) {
            request.tools.map { tool ->
                val params = try {
                    json.parseToJsonElement(tool.inputSchemaJson).jsonObject
                } catch (e: Exception) {
                    JsonObject(emptyMap())
                }
                OpenAiTool(
                    function = OpenAiFunction(
                        name = tool.name,
                        description = tool.description,
                        parameters = params
                    )
                )
            }
        } else null

        val chatReq = OpenAiChatRequest(
            model = request.model,
            messages = messages,
            maxTokens = request.maxTokens,
            temperature = request.temperature,
            stream = true,
            tools = tools
        )

        return json.encodeToString(OpenAiChatRequest.serializer(), chatReq)
    }
}
