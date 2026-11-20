package com.agentdesk.app.core.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRedactor @Inject constructor() {

    private val patterns = listOf(
        // API keys starting with sk-
        Regex("sk-[a-zA-Z0-9_\\-]{16,}") to "sk-[REDACTED]",
        // Bearer tokens
        Regex("(?i)Bearer\\s+[a-zA-Z0-9_\\-\\.]+") to "Bearer [REDACTED]",
        // Authorization headers or key-value pairs
        Regex("(?i)(api[_-]?key|secret|password|token)[\"']?\\s*[:=]\\s*[\"']?([a-zA-Z0-9_\\-\\.]+)[\"']?") to "$1=[REDACTED]",
        // Anthropic sk-ant- keys
        Regex("sk-ant-[a-zA-Z0-9_\\-]{16,}") to "sk-ant-[REDACTED]",
        // Google AI Studio / Gemini keys
        Regex("AIza[0-9A-Za-z-_]{35}") to "AIza[REDACTED]"
    )

    fun redact(input: String): String {
        var result = input
        for ((regex, replacement) in patterns) {
            result = regex.replace(result, replacement)
        }
        return result
    }
}
