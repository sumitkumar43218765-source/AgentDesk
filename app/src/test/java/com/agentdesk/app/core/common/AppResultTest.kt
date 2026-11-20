package com.agentdesk.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun success_getOrNull_returnsValue() {
        val result: AppResult<String> = AppResult.Success("hello")
        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun failure_errorOrNull_returnsError() {
        val error = AppError.Network("Connection timed out")
        val result: AppResult<String> = AppResult.Failure(error)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun map_transformsSuccessValue() {
        val result: AppResult<Int> = AppResult.Success(10)
        val mapped = result.map { it * 2 }
        assertEquals(20, mapped.getOrNull())
    }

    @Test
    fun flatMap_chainsResults() {
        val result: AppResult<Int> = AppResult.Success(10)
        val flatMapped = result.flatMap { AppResult.Success("Count: $it") }
        assertEquals("Count: 10", flatMapped.getOrNull())
    }
}
