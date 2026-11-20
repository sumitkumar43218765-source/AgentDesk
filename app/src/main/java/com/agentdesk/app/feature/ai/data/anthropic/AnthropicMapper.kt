package com.agentdesk.app.feature.ai.data.anthropic

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.Role
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnthropicMapper @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun toRequestBodyJson(request: LlmRequest): String {
        val messages = request.messages.map { msg ->
            val role = when (msg.role) {
                Role.USER -> "user"
                Role.ASSISTANT -> "assistant"
            }
            val contentBlocks = msg.blocks.map { block ->
                when (block) {
                    is ContentBlock.Text -> AnthropicContentBlock(
                        type = "text",
                        text = block.text
                    )
                    is ContentBlock.ToolUse -> {
                        val inputElement = try {
                            json.parseToJsonElement(block.argsJson)
                        } catch (e: Exception) {
                            JsonObject(emptyMap())
                        }
                        AnthropicContentBlock(
                            type = "tool_use",
                            id = block.id,
                            name = block.name,
                            input = inputElement
                        )
                    }
                    is ContentBlock.ToolResult -> AnthropicContentBlock(
                        type = "tool_result",
                        toolUseId = block.toolUseId,
                        content = block.content,
                        isError = block.isError
                    )
                }
            }
            AnthropicMessage(role = role, content = contentBlocks)
        }

        val tools = if (request.tools.isNotEmpty()) {
            request.tools.map { tool ->
                val schemaObject = try {
                    json.parseToJsonElement(tool.inputSchemaJson).jsonObject
                } catch (e: Exception) {
                    JsonObject(emptyMap())
                }
                AnthropicTool(
                    name = tool.name,
                    description = tool.description,
                    inputSchema = schemaObject
                )
            }
        } else null

        val req = AnthropicMessageRequest(
            model = request.model,
            messages = messages,
            maxTokens = request.maxTokens,
            system = request.system,
            stream = true,
            temperature = request.temperature,
            tools = tools
        )

        return json.encodeToString(AnthropicMessageRequest.serializer(), req)
    }
}
