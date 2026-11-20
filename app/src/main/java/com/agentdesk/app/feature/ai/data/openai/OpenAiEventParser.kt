package com.agentdesk.app.feature.ai.data.openai

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.feature.ai.data.ToolCallAssembler
import com.agentdesk.app.feature.ai.data.http.SseEvent
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.StopReason
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiEventParser @Inject constructor(
    private val toolCallAssembler: ToolCallAssembler
) {

    fun parse(sseEvent: SseEvent): List<LlmEvent> {
        val data = sseEvent.data.trim()
        if (data.isBlank()) return emptyList()

        if (data == "[DONE]") {
            return listOf(LlmEvent.Done(StopReason.END_TURN))
        }

        val events = mutableListOf<LlmEvent>()

        try {
            val json = JSONObject(data)

            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                val message = errorObj.optString("message", "OpenAI Error")
                return listOf(LlmEvent.Error(AppError.Unknown(message)))
            }

            // Parse token usage if streamed
            val usageObj = json.optJSONObject("usage")
            if (usageObj != null) {
                val promptTokens = usageObj.optInt("prompt_tokens", 0)
                val completionTokens = usageObj.optInt("completion_tokens", 0)
                events.add(LlmEvent.Usage(promptTokens, completionTokens))
            }

            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val delta = firstChoice.optJSONObject("delta")
                val finishReason = firstChoice.optString("finish_reason", null)

                if (delta != null) {
                    val content = delta.optString("content", null)
                    if (!content.isNullOrEmpty()) {
                        events.add(LlmEvent.TextDelta(content))
                    }

                    val toolCalls = delta.optJSONArray("tool_calls")
                    if (toolCalls != null) {
                        for (i in 0 until toolCalls.length()) {
                            val tc = toolCalls.getJSONObject(i)
                            val id = tc.optString("id", null)
                            val function = tc.optJSONObject("function")
                            val name = function?.optString("name", null)
                            val arguments = function?.optString("arguments", null)

                            if (id != null && name != null) {
                                toolCallAssembler.onToolCallStarted(id, name)
                                events.add(LlmEvent.ToolCallStarted(id, name))
                            }

                            if (!arguments.isNullOrEmpty()) {
                                val targetId = id ?: "active_tool"
                                toolCallAssembler.onArgsDelta(targetId, arguments)
                                events.add(LlmEvent.ToolCallArgsDelta(targetId, arguments))
                            }
                        }
                    }
                }

                if (!finishReason.isNullOrEmpty()) {
                    val reason = when (finishReason) {
                        "stop" -> StopReason.END_TURN
                        "tool_calls" -> {
                            val assembled = toolCallAssembler.completeToolCall("active_tool")
                            if (assembled.argsJson.isNotEmpty()) {
                                events.add(LlmEvent.ToolCallCompleted(assembled.id, assembled.name, assembled.argsJson))
                            }
                            StopReason.TOOL_USE
                        }
                        "length" -> StopReason.MAX_TOKENS
                        else -> StopReason.OTHER
                    }
                    events.add(LlmEvent.Done(reason))
                }
            }
        } catch (e: Exception) {
            // Ignore malformed partial frame
        }

        return events
    }
}
