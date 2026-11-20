package com.agentdesk.app.feature.ai

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.feature.ai.data.http.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ErrorMapperTest {

    private lateinit var errorMapper: ErrorMapper

    @Before
    fun setUp() {
        errorMapper = ErrorMapper()
    }

    @Test
    fun map_401Unauthorized_mapsToAuthError() {
        val body = """{"error": {"message": "Invalid API Key"}}"""
        val error = errorMapper.map(401, body)

        assertTrue(error is AppError.Auth)
        assertEquals("Invalid API Key", error.message)
    }

    @Test
    fun map_429RateLimit_mapsToRateLimitErrorWithRetryAfter() {
        val body = """{"error": {"message": "Too many requests"}, "retry_after": 5}"""
        val error = errorMapper.map(429, body)

        assertTrue(error is AppError.RateLimit)
        assertEquals(5000L, (error as AppError.RateLimit).retryAfterMs)
    }

    @Test
    fun map_500InternalServer_mapsToNetworkError() {
        val error = errorMapper.map(500, "Internal Server Error")
        assertTrue(error is AppError.Network)
    }

    @Test
    fun map_contextLengthExceeded_mapsToContextTooLong() {
        val body = """{"error": {"message": "prompt is too long for this model"}}"""
        val error = errorMapper.map(400, body)

        assertTrue(error is AppError.Unknown)
        assertTrue(error.message.startsWith("CONTEXT_TOO_LONG"))
    }
}
