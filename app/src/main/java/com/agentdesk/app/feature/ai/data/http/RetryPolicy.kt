package com.agentdesk.app.feature.ai.data.http

import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

@Singleton
class RetryPolicy @Inject constructor() {

    companion object {
        const val MAX_RETRIES = 3
        const val INITIAL_BACKOFF_MS = 1000L
        const val MAX_BACKOFF_MS = 8000L
    }

    suspend fun <T> executeWithRetry(
        hasEmittedPartial: () -> Boolean = { false },
        block: suspend (attempt: Int) -> T
    ): T {
        var currentAttempt = 0
        var currentDelay = INITIAL_BACKOFF_MS

        while (true) {
            try {
                return block(currentAttempt)
            } catch (e: Exception) {
                currentAttempt++

                // Never retry if partial output was already emitted to user or max retries exceeded
                if (hasEmittedPartial() || currentAttempt >= MAX_RETRIES || !isRetryable(e)) {
                    throw e
                }

                // Exponential backoff with jitter
                val jitter = Random.nextLong(0, 500)
                delay(currentDelay + jitter)
                currentDelay = min(currentDelay * 2, MAX_BACKOFF_MS)
            }
        }
    }

    fun isRetryable(throwable: Throwable): Boolean {
        return throwable is IOException || throwable.message?.contains("429") == true || throwable.message?.contains("50") == true
    }
}
