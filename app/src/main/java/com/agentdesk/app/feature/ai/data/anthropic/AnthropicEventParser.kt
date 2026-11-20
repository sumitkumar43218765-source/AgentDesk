package com.agentdesk.app.feature.ai.data.anthropic

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.feature.ai.data.ToolCallAssembler
import com.agentdesk.app.feature.ai.data.http.SseEvent
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.StopReason
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnthropicEventParser @Inject constructor(
    private val toolCallAssembler: ToolCallAssembler
) {

    fun parse(sseEvent: SseEvent): List<LlmEvent> {
        val eventType = sseEvent.event ?: return emptyList()
        val data = sseEvent.data
        if (data.isBlank() || data == "[DONE]") return emptyList()

        val events = mutableListOf<LlmEvent>()

        try {
            val json = JSONObject(data)

            when (eventType) {
                "message_start" -> {
                    val message = json.optJSONObject("message")
                    val usage = message?.optJSONObject("usage")
                    if (usage != null) {
                        val inputTokens = usage.optInt("input_tokens", 0)
                        val outputTokens = usage.optInt("output_tokens", 0)
                        events.add(LlmEvent.Usage(inputTokens, outputTokens))
                    }
                }
                "content_block_start" -> {
                    val contentBlock = json.optJSONObject("content_block")
                    if (contentBlock != null) {
                        val type = contentBlock.optString("type")
                        if (type == "tool_use") {
                            val id = contentBlock.optString("id")
                            val name = contentBlock.optString("name")
                            toolCallAssembler.onToolCallStarted(id, name)
                            events.add(LlmEvent.ToolCallStarted(id, name))
                        }
                    }
                }
                "content_block_delta" -> {
                    val delta = json.optJSONObject("delta")
                    if (delta != null) {
                        val type = delta.optString("type")
                        when (type) {
                            "text_delta" -> {
                                val text = delta.optString("text")
                                if (text.isNotEmpty()) {
                                    events.add(LlmEvent.TextDelta(text))
                                }
                            }
                            "input_json_delta" -> {
                                val partial = delta.optString("partial_json")
                                val index = json.optInt("index", 0)
                                val toolId = "block_$index"
                                toolCallAssembler.onArgsDelta(toolId, partial)
                                events.add(LlmEvent.ToolCallArgsDelta(toolId, partial))
                            }
                        }
                    }
                }
                "content_block_stop" -> {
                    val index = json.optInt("index", 0)
                    val toolId = "block_$index"
                    val assembled = toolCallAssembler.completeToolCall(toolId)
                    if (assembled.argsJson.isNotEmpty() && assembled.name != "unknown_tool") {
                        events.add(LlmEvent.ToolCallCompleted(assembled.id, assembled.name, assembled.argsJson))
                    }
                }
                "message_delta" -> {
                    val delta = json.optJSONObject("delta")
                    val stopReasonStr = delta?.optString("stop_reason")
                    val usage = json.optJSONObject("usage")
                    if (usage != null) {
                        val outputTokens = usage.optInt("output_tokens", 0)
                        events.add(LlmEvent.Usage(0, outputTokens))
                    }

                    if (!stopReasonStr.isNullOrEmpty()) {
                        val reason = when (stopReasonStr) {
                            "end_turn" -> StopReason.END_TURN
                            "tool_use" -> StopReason.TOOL_USE
                            "max_tokens" -> StopReason.MAX_TOKENS
                            else -> StopReason.OTHER
                        }
                        events.add(LlmEvent.Done(reason))
                    }
                }
                "message_stop" -> {
                    // Stream finished
                }
                "error" -> {
                    val errorObj = json.optJSONObject("error")
                    val message = errorObj?.optString("message") ?: "Anthropic error"
                    events.add(LlmEvent.Error(AppError.Unknown(message)))
                }
            }
        } catch (e: Exception) {
            // Keep going on malformed single frame
        }

        return events
    }
}
