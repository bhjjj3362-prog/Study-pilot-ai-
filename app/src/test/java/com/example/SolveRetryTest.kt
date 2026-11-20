package com.example

import com.example.data.GeminiModelOption
import com.example.data.SolveAttemptProgress
import com.example.data.executeWithRetryAndFallback
import com.example.data.isRetryableBusyOrTimeoutError
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolveRetryTest {

    @Test
    fun `retries up to 2 times on 503 error and succeeds on 3rd attempt`() = runTest {
        var calls = 0
        val recordedProgress = mutableListOf<SolveAttemptProgress>()

        val (answeredModel, result) = executeWithRetryAndFallback(
            modelsInOrder = GeminiModelOption.fallbackOrderStartingFrom(
                GeminiModelOption.GEMINI_3_5_FLASH_LITE
            ),
            maxRetriesPerModel = 2,
            onProgress = { recordedProgress.add(it) },
            delayFn = { },
            logWarning = { _, _ -> },
        ) { model ->
            calls++
            if (calls < 3) {
                throw IllegalStateException("HTTP 503 Service Unavailable")
            }
            "Solved by ${model.modelId}"
        }

        assertEquals(GeminiModelOption.GEMINI_3_5_FLASH_LITE, answeredModel)
        assertEquals("Solved by gemini-3.5-flash-lite", result)
        assertEquals(3, calls)
        assertEquals(listOf(1, 2, 3), recordedProgress.map { it.attempt })
        assertTrue(recordedProgress.none { it.isFallback })
    }

    @Test
    fun `falls back through model chain on persistent 429 and timeout`() = runTest {
        val invokedModels = mutableListOf<String>()
        val progressList = mutableListOf<SolveAttemptProgress>()

        val (answeredModel, result) = executeWithRetryAndFallback(
            modelsInOrder = GeminiModelOption.fallbackOrderStartingFrom(
                GeminiModelOption.GEMINI_3_5_FLASH_LITE
            ),
            maxRetriesPerModel = 2,
            onProgress = { progressList.add(it) },
            delayFn = { },
            logWarning = { _, _ -> },
        ) { model ->
            invokedModels.add(model.modelId)
            when (model) {
                GeminiModelOption.GEMINI_3_5_FLASH_LITE ->
                    throw IllegalStateException("HTTP 429 Too Many Requests")
                GeminiModelOption.GEMINI_3_1_FLASH_LITE ->
                    throw SocketTimeoutException("Read timed out")
                else -> "Fallback answer"
            }
        }

        assertEquals(GeminiModelOption.GEMINI_3_6_FLASH, answeredModel)
        assertEquals("Fallback answer", result)
        assertEquals(
            listOf(
                "gemini-3.5-flash-lite",
                "gemini-3.5-flash-lite",
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3.6-flash",
            ),
            invokedModels,
        )
        assertTrue(progressList.last().isFallback)
    }

    @Test
    fun `fallback order starts with selected model then remaining models`() {
        val order = GeminiModelOption.fallbackOrderStartingFrom(GeminiModelOption.GEMINI_3_6_FLASH)
        assertEquals(
            listOf(
                GeminiModelOption.GEMINI_3_6_FLASH,
                GeminiModelOption.GEMINI_3_5_FLASH_LITE,
                GeminiModelOption.GEMINI_3_1_FLASH_LITE,
                GeminiModelOption.GEMINI_3_8_FLASH,
                GeminiModelOption.GEMINI_4_HYDROGEN,
            ),
            order,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `does not retry or fall back on non-retryable client error`() = runTest {
        var calls = 0
        try {
            executeWithRetryAndFallback(
                modelsInOrder = GeminiModelOption.entries,
                maxRetriesPerModel = 2,
                delayFn = { },
                logWarning = { _, _ -> },
            ) {
                calls++
                throw IllegalArgumentException("HTTP 400 Bad Request")
            }
        } finally {
            assertEquals(1, calls)
        }
    }

    @Test
    fun `isRetryableBusyOrTimeoutError identifies timeout, 429, and 503 causes`() {
        assertTrue(isRetryableBusyOrTimeoutError(RuntimeException("wrapped", SocketTimeoutException())))
        assertTrue(isRetryableBusyOrTimeoutError(RuntimeException("HTTP 429 RESOURCE_EXHAUSTED")))
        assertTrue(isRetryableBusyOrTimeoutError(RuntimeException("503 UNAVAILABLE")))
        assertFalse(isRetryableBusyOrTimeoutError(IllegalArgumentException("Invalid argument")))
        assertFalse(isRetryableBusyOrTimeoutError(CancellationException("cancelled")))
    }
}
