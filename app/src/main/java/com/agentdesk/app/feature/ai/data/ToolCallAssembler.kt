package com.agentdesk.app.feature.ai.data

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolCallAssembler @Inject constructor() {

    private val buffers = mutableMapOf<String, StringBuilder>()
    private val names = mutableMapOf<String, String>()

    @Synchronized
    fun onToolCallStarted(id: String, name: String) {
        buffers[id] = StringBuilder()
        names[id] = name
    }

    @Synchronized
    fun onArgsDelta(id: String, delta: String) {
        val buffer = buffers.getOrPut(id) { StringBuilder() }
        buffer.append(delta)
    }

    @Synchronized
    fun completeToolCall(id: String, fallbackName: String? = null): ToolCallResult {
        val name = names[id] ?: fallbackName ?: "unknown_tool"
        val rawJson = buffers[id]?.toString().orEmpty()
        buffers.remove(id)
        names.remove(id)

        val isValidJson = try {
            if (rawJson.isBlank()) {
                true
            } else {
                JSONObject(rawJson)
                true
            }
        } catch (e: Exception) {
            false
        }

        return ToolCallResult(
            id = id,
            name = name,
            argsJson = if (rawJson.isBlank()) "{}" else rawJson,
            isValidJson = isValidJson
        )
    }

    @Synchronized
    fun clear() {
        buffers.clear()
        names.clear()
    }

    data class ToolCallResult(
        val id: String,
        val name: String,
        val argsJson: String,
        val isValidJson: Boolean
    )
}
