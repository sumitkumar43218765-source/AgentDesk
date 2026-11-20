package com.agentdesk.app.feature.chat.data.local

import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.TokenUsage
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocksJsonConverter @Inject constructor() {

    fun blocksToJson(blocks: List<ContentBlock>): String {
        val array = JSONArray()
        for (block in blocks) {
            val obj = JSONObject()
            when (block) {
                is ContentBlock.Text -> {
                    obj.put("type", "text")
                    obj.put("text", block.text)
                }
                is ContentBlock.ToolUse -> {
                    obj.put("type", "tool_use")
                    obj.put("id", block.id)
                    obj.put("name", block.name)
                    obj.put("argsJson", block.argsJson)
                }
                is ContentBlock.ToolResult -> {
                    obj.put("type", "tool_result")
                    obj.put("toolUseId", block.toolUseId)
                    obj.put("content", block.content)
                    obj.put("isError", block.isError)
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToBlocks(json: String): List<ContentBlock> {
        if (json.isBlank()) return emptyList()
        val blocks = mutableListOf<ContentBlock>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                when (obj.optString("type")) {
                    "text" -> blocks.add(ContentBlock.Text(obj.getString("text")))
                    "tool_use" -> blocks.add(
                        ContentBlock.ToolUse(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            argsJson = obj.optString("argsJson", "{}")
                        )
                    )
                    "tool_result" -> blocks.add(
                        ContentBlock.ToolResult(
                            toolUseId = obj.getString("toolUseId"),
                            content = obj.getString("content"),
                            isError = obj.optBoolean("isError", false)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback for simple raw string
            blocks.add(ContentBlock.Text(json))
        }
        return blocks
    }

    fun usageToJson(usage: TokenUsage?): String? {
        if (usage == null) return null
        return JSONObject().apply {
            put("input", usage.inputTokens)
            put("output", usage.outputTokens)
        }.toString()
    }

    fun jsonToUsage(json: String?): TokenUsage? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            TokenUsage(
                inputTokens = obj.optInt("input", 0),
                outputTokens = obj.optInt("output", 0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
