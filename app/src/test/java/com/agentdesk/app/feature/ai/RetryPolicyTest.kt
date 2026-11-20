package com.agentdesk.app.feature.ai

import com.agentdesk.app.feature.ai.data.http.RetryPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RetryPolicyTest {

    private lateinit var retryPolicy: RetryPolicy

    @Before
    fun setUp() {
        retryPolicy = RetryPolicy()
    }

    @Test
    fun executeWithRetry_succeedsAfterOneFailure() = runTest {
        var attempts = 0
        val result = retryPolicy.executeWithRetry { attempt ->
            attempts++
            if (attempt == 0) throw IOException("Temporary glitch")
            "Success"
        }

        assertEquals("Success", result)
        assertEquals(2, attempts)
    }

    @Test(expected = IOException::class)
    fun executeWithRetry_partialAlreadyEmitted_doesNotRetry() = runTest {
        var hasEmitted = false
        retryPolicy.executeWithRetry(hasEmittedPartial = { hasEmitted }) { attempt ->
            if (attempt == 0) {
                hasEmitted = true
                throw IOException("Connection reset mid-stream")
            }
            "Should not reach"
        }
    }
}
