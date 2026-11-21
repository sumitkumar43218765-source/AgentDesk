package com.agentdesk.app.feature.agent.data.runtime

import com.agentdesk.app.feature.agent.domain.model.LoopGuardResult
import kotlinx.serialization.json.JsonObject

class LoopGuard(
    private val maxConsecutiveToolErrors: Int = 5,
    private val repeatWarningThreshold: Int = 3,
    private val repeatAbortThreshold: Int = 5
) {
    private val callHistory = mutableListOf<Pair<String, String>>()
    private var consecutiveErrors = 0

    fun checkBeforeExecution(toolName: String, args: JsonObject): LoopGuardResult {
        val key = Pair(toolName, args.toString())
        val count = callHistory.count { it == key } + 1
        return when {
            count >= repeatAbortThreshold -> {
                LoopGuardResult.Abort("Tool '$toolName' was invoked $count times with identical arguments. Stopping runaway loop.")
            }
            count >= repeatWarningThreshold -> {
                LoopGuardResult.Warning("Warning: Tool '$toolName' was invoked $count times with identical arguments. Please adjust your strategy or check results.")
            }
            else -> LoopGuardResult.Pass
        }
    }

    fun recordExecution(toolName: String, args: JsonObject, isError: Boolean): LoopGuardResult {
        val key = Pair(toolName, args.toString())
        callHistory.add(key)
        if (isError) {
            consecutiveErrors++
            if (consecutiveErrors >= maxConsecutiveToolErrors) {
                return LoopGuardResult.Abort("Exceeded maximum consecutive tool errors ($maxConsecutiveToolErrors). Stopping run.")
            }
        } else {
            consecutiveErrors = 0
        }
        return LoopGuardResult.Pass
    }

    fun reset() {
        callHistory.clear()
        consecutiveErrors = 0
    }
}
