package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MAX_HISTORY_ITEMS
import com.example.data.SolveAnswer
import com.example.data.SolveHistoryItem
import com.example.data.SolveHistoryStore
import com.example.data.appendHistoryCapped
import com.example.data.decodeHistoryJson
import com.example.data.encodeHistoryJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SolveHistoryTest {

    private fun sampleItem(index: Int): SolveHistoryItem = SolveHistoryItem(
        id = "id-$index",
        question = "Question $index",
        hasPhoto = index % 2 == 0,
        answer = SolveAnswer(
            analogy = "Analogy sentence one $index. Analogy sentence two $index.",
            stepByStepLogic = listOf("Step A $index", "Step B $index"),
            commonExamTrap = "Trap $index",
        ),
        requestedModelId = "gemini-3.5-flash-lite",
        answeredModelId = if (index == 3) "gemini-3.1-flash-lite" else "gemini-3.5-flash-lite",
        timestampMillis = 1_700_000_000_000L + index,
    )

    @Test
    fun `caps history at 5 most recent items`() {
        var items = emptyList<SolveHistoryItem>()
        for (i in 1..7) {
            items = appendHistoryCapped(items, sampleItem(i), MAX_HISTORY_ITEMS)
        }

        assertEquals(5, items.size)
        assertEquals(listOf("id-7", "id-6", "id-5", "id-4", "id-3"), items.map { it.id })
    }

    @Test
    fun `encodes and decodes 3-card answers losslessly`() {
        val original = listOf(sampleItem(1), sampleItem(3))
        val encoded = encodeHistoryJson(original)
        val decoded = decodeHistoryJson(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `persists and clears history in local DataStore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SolveHistoryStore(context)
        store.clear()

        for (i in 1..6) {
            store.saveItem(sampleItem(i))
        }

        val saved = store.historyFlow.first()
        assertEquals(5, saved.size)
        assertEquals("Question 6", saved.first().question)
        assertEquals("Question 2", saved.last().question)

        store.clear()
        assertTrue(store.historyFlow.first().isEmpty())
    }
}
