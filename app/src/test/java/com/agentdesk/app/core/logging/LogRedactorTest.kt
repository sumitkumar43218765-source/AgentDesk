package com.agentdesk.app.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogRedactorTest {

    private lateinit var redactor: LogRedactor

    @Before
    fun setUp() {
        redactor = LogRedactor()
    }

    @Test
    fun redact_skKey_isRedacted() {
        val input = "Request failed with key: sk-ant-api03-abcdef1234567890abcdef123456"
        val result = redactor.redact(input)
        assertFalse(result.contains("abcdef1234567890"))
        assertTrue(result.contains("sk-[REDACTED]") || result.contains("sk-ant-[REDACTED]"))
    }

    @Test
    fun redact_bearerToken_isRedacted() {
        val input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz123"
        val result = redactor.redact(input)
        assertFalse(result.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz123"))
        assertTrue(result.contains("Bearer [REDACTED]"))
    }

    @Test
    fun redact_apiKeyHeader_isRedacted() {
        val input = "headers: api_key=secret_value_12345678"
        val result = redactor.redact(input)
        assertFalse(result.contains("secret_value_12345678"))
        assertTrue(result.contains("api_key=[REDACTED]"))
    }

    @Test
    fun redact_safeMessage_remainsUnchanged() {
        val input = "Normal informational message without secrets"
        val result = redactor.redact(input)
        assertEquals(input, result)
    }
}
