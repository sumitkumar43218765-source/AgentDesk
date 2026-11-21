package com.agentdesk.app.feature.agent.data.runtime

import com.agentdesk.app.feature.agent.domain.ContextManager
import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.LlmMessage
import com.agentdesk.app.feature.build.data.parse.LogDistiller
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultContextManager @Inject constructor() : ContextManager {

    override fun prepareMessages(
        systemPrompt: String,
        messages: List<ChatMessage>,
        maxContextTokens: Int
    ): List<LlmMessage> {
        val converted = messages.map { chatMsg ->
            LlmMessage(
                role = chatMsg.role,
                blocks = chatMsg.blocks
            )
        }.toMutableList()

        var currentTokens = estimateTokens(systemPrompt, converted)
        val budgetLimit = (maxContextTokens * 0.70).toInt()

        // 1. If exceeding 70% threshold, replace older ToolResult blocks with stubs (preserve error results)
        if (currentTokens > budgetLimit && converted.size > 2) {
            for (i in 0 until (converted.size - 2)) {
                val msg = converted[i]
                val stubbedBlocks = msg.blocks.map { block ->
                    if (block is ContentBlock.ToolResult && !block.isError) {
                        ContentBlock.ToolResult(
                            toolUseId = block.toolUseId,
                            content = "[Previous tool output omitted for brevity]",
                            isError = false
                        )
                    } else {
                        block
                    }
                }
                converted[i] = msg.copy(blocks = stubbedBlocks)
            }
            currentTokens = estimateTokens(systemPrompt, converted)
        }

        // 2. If still exceeding budget, drop oldest messages (leaving at least the most recent turn)
        while (currentTokens > budgetLimit && converted.size > 2) {
            converted.removeAt(0)
            currentTokens = estimateTokens(systemPrompt, converted)
        }

        return converted
    }

    override fun trimToolOutput(output: String, maxChars: Int): String {
        if (output.length <= maxChars) return output
        val distilled = LogDistiller.distill(output, maxChars)
        val text = distilled.text
        return if (text.length > maxChars) {
            text.take(maxChars) + "\n...[Output truncated to $maxChars characters]"
        } else {
            text + "\n...[Output truncated to $maxChars characters]"
        }
    }

    private fun estimateTokens(systemPrompt: String, messages: List<LlmMessage>): Int {
        var charCount = systemPrompt.length
        for (msg in messages) {
            for (block in msg.blocks) {
                when (block) {
                    is ContentBlock.Text -> charCount += block.text.length
                    is ContentBlock.ToolUse -> charCount += (block.name.length + block.argsJson.length)
                    is ContentBlock.ToolResult -> charCount += block.content.length
                }
            }
        }
        return (charCount + 3) / 4
    }
}
