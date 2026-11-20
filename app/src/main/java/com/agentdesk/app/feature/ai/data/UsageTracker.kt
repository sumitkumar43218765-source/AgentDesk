package com.agentdesk.app.feature.ai.data

import com.agentdesk.app.feature.ai.domain.model.TokenUsage
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTracker @Inject constructor() {

    private val inputTokens = AtomicInteger(0)
    private val outputTokens = AtomicInteger(0)

    fun recordUsage(input: Int, output: Int) {
        inputTokens.addAndGet(input)
        outputTokens.addAndGet(output)
    }

    fun getUsage(): TokenUsage {
        return TokenUsage(
            inputTokens = inputTokens.get(),
            outputTokens = outputTokens.get()
        )
    }

    fun reset() {
        inputTokens.set(0)
        outputTokens.set(0)
    }
}
