package com.example

import com.example.data.DemoRecallData
import com.example.data.parseRecallStudySetJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecallParsingTest {

    @Test
    fun `demo study set provides 3 science flashcards and short interactive quiz`() {
        val demo = DemoRecallData.sampleStudySet()
        assertTrue(demo.isDemo)
        assertFalse(demo.usedFallback)
        assertEquals(3, demo.flashcards.size)
        assertEquals(3, demo.quizQuestions.size)
        assertTrue(demo.quizQuestions.all { it.options.size == 4 && it.correctIndex in 0..3 })
    }

    @Test
    fun `parses 5 flashcards and 5 MCQs from structured JSON and tracks fallback`() {
        val cardsJson = (1..5).joinToString(",") { i ->
            """{"question":"Q$i","answer":"A$i"}"""
        }
        val quizJson = (1..5).joinToString(",") { i ->
            """{"question":"MCQ $i","options":["O1","O2","O3","O4"],"correctIndex":${i - 1},"explanation":"Because $i."}"""
        }
        val raw = """{"topicTitle":"Thermodynamics","flashcards":[$cardsJson],"quiz":[$quizJson]}"""

        val parsed = parseRecallStudySetJson(
            rawJson = raw,
            requestedModelId = "gemini-3.5-flash-lite",
            answeredModelId = "gemini-3.6-flash",
        )

        assertEquals("Thermodynamics", parsed.topicTitle)
        assertEquals(5, parsed.flashcards.size)
        assertEquals(5, parsed.quizQuestions.size)
        // Index 4 is clamped to 3 (lastIndex of 4 options)
        assertEquals(3, parsed.quizQuestions.last().correctIndex)
        assertTrue(parsed.usedFallback)
        assertFalse(parsed.isDemo)
    }
}
