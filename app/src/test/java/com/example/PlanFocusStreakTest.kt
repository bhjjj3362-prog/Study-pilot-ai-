package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DemoPlanData
import com.example.data.FocusStore
import com.example.data.MAX_PLAN_DAYS
import com.example.data.PlanStore
import com.example.data.StudyDateUtils
import com.example.data.StudyStreakStore
import com.example.data.buildDistributedPlanTasks
import com.example.data.computeEffectiveStreak
import com.example.data.computeFocusSessionsForToday
import com.example.data.computeStreakOnActivity
import com.example.data.decodeRevisionPlanJson
import com.example.data.encodeRevisionPlanJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlanFocusStreakTest {

    @Test
    fun `daysBetweenIso and addDaysIso compute exact remaining days`() {
        assertEquals("2026-11-26", StudyDateUtils.addDaysIso("2026-11-19", 7))
        assertEquals(7, StudyDateUtils.daysBetweenIso("2026-11-19", "2026-11-26"))
        assertEquals(0, StudyDateUtils.daysBetweenIso("2026-11-19", "2026-11-10"))
    }

    @Test
    fun `buildDistributedPlanTasks caps at 30 days and distributes subjects evenly`() {
        val tasks = buildDistributedPlanTasks(
            subjects = listOf("Physics", "Chemistry", "Calculus"),
            daysRemaining = 45,
            dailyHours = 3.0,
        )
        assertEquals(MAX_PLAN_DAYS, tasks.size)
        assertEquals(10, tasks.count { it.subject == "Physics" })
        assertEquals(10, tasks.count { it.subject == "Chemistry" })
        assertEquals(10, tasks.count { it.subject == "Calculus" })
    }

    @Test
    fun `demo 7-day plan pre-loads 7 distributed science and math tasks`() {
        val sample = DemoPlanData.samplePlan("2026-11-19")
        assertEquals(7, sample.tasks.size)
        assertEquals("2026-11-26", sample.targetDateIso)
        assertTrue(sample.isSample)
    }

    @Test
    fun `plan json and DataStore preserve check states across reloads`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PlanStore(context)
        val sample = DemoPlanData.samplePlan("2026-11-19")
        assertEquals(0, sample.completionPercent)
        store.savePlan(sample)

        val roundTripped = decodeRevisionPlanJson(encodeRevisionPlanJson(sample))
        assertEquals(sample, roundTripped)

        val becameCompleted = store.toggleTask(sample.tasks.first().id)
        assertTrue(becameCompleted)
        val loaded = store.planFlow.first()
        assertTrue(loaded.tasks.first().isCompleted)
        assertEquals(1, loaded.completedCount)
        assertEquals(14, loaded.completionPercent)
    }

    @Test
    fun `streak increments once per calendar day and resets after missed day`() {
        // First activity ever -> streak 1
        val (s1, d1) = computeStreakOnActivity(0, "", "2026-11-19", "2026-11-18")
        assertEquals(1, s1)
        assertEquals("2026-11-19", d1)

        // Same day second activity -> stays 1
        val (sSame, _) = computeStreakOnActivity(s1, d1, "2026-11-19", "2026-11-18")
        assertEquals(1, sSame)

        // Next consecutive day -> increments to 2
        val (s2, d2) = computeStreakOnActivity(sSame, d1, "2026-11-20", "2026-11-19")
        assertEquals(2, s2)
        assertEquals("2026-11-20", d2)

        // Missed a day -> effective streak drops to 0 and next activity starts at 1
        val effective = computeEffectiveStreak(s2, d2, "2026-11-22", "2026-11-21")
        assertEquals(0, effective.currentStreak)
        assertFalse(effective.isActiveToday)

        val (sReset, _) = computeStreakOnActivity(s2, d2, "2026-11-22", "2026-11-21")
        assertEquals(1, sReset)
    }

    @Test
    fun `focus store tracks today count and resets on new date`() = runTest {
        assertEquals(3, computeFocusSessionsForToday(3, "2026-11-19", "2026-11-19"))
        assertEquals(0, computeFocusSessionsForToday(3, "2026-11-18", "2026-11-19"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val focusStore = FocusStore(context)
        val streakStore = StudyStreakStore(context)

        focusStore.incrementCompletedSessionToday()
        streakStore.recordQualifyingActivity()

        assertTrue(focusStore.focusStatsFlow.first().completedToday >= 1)
        assertTrue(streakStore.streakFlow.first().currentStreak >= 1)
        assertTrue(streakStore.streakFlow.first().isActiveToday)
    }
}
