package com.agentdesk.app.feature.ai.data.anthropic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicMessageRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String? = null,
    val stream: Boolean = true,
    val temperature: Double? = null,
    val tools: List<AnthropicTool>? = null
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContentBlock>
)

@Serializable
data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("tool_use_id") val toolUseId: String? = null,
    val content: String? = null,
    @SerialName("is_error") val isError: Boolean? = null
)

@Serializable
data class AnthropicTool(
    val name: String,
    val description: String,
    @SerialName("input_schema") val inputSchema: kotlinx.serialization.json.JsonObject
)
