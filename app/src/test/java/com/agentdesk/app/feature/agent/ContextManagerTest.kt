package com.agentdesk.app.feature.agent

import com.agentdesk.app.feature.agent.data.runtime.DefaultContextManager
import com.agentdesk.app.feature.ai.domain.model.ContentBlock
import com.agentdesk.app.feature.ai.domain.model.Role
import com.agentdesk.app.feature.chat.domain.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextManagerTest {

    private lateinit var contextManager: DefaultContextManager

    @Before
    fun setUp() {
        contextManager = DefaultContextManager()
    }

    @Test
    fun `trimToolOutput caps output to maxChars`() {
        val longText = "A".repeat(12000)
        val trimmed = contextManager.trimToolOutput(longText, maxChars = 2000)
        assertTrue(trimmed.length <= 2100) // 2000 chars + truncation notice
        assertTrue(trimmed.contains("Output truncated"))
    }

    @Test
    fun `prepareMessages stubs older tool results when token limit exceeded`() {
        val systemPrompt = "System prompt"
        // Create 10 messages with large tool outputs
        val messages = mutableListOf<ChatMessage>()
        for (i in 1..10) {
            messages.add(
                ChatMessage(
                    id = "msg_$i",
                    conversationId = "conv_1",
                    role = Role.USER,
                    blocks = listOf(
                        ContentBlock.ToolResult(
                            toolUseId = "call_$i",
                            content = "Very large tool output result #$i ".repeat(100),
                            isError = false
                        )
                    )
                )
            )
        }

        // Token budget sufficient to keep messages after stubbing
        val prepared = contextManager.prepareMessages(
            systemPrompt = systemPrompt,
            messages = messages,
            maxContextTokens = 5000
        )

        // Earlier messages should have tool output stubbed
        val firstMsgBlock = prepared.first().blocks.first()
        assertTrue(firstMsgBlock is ContentBlock.ToolResult)
        assertEquals(
            "[Previous tool output omitted for brevity]",
            (firstMsgBlock as ContentBlock.ToolResult).content
        )
    }
}
