package com.example.data

const val MAX_PLAN_DAYS = 30

data class PlanTask(
    val id: String,
    val dayNumber: Int,
    val subject: String,
    val description: String,
    val hoursLabel: String,
    val isCompleted: Boolean = false,
)

data class RevisionPlan(
    val examName: String,
    val targetDateIso: String,
    val subjectsRaw: String,
    val dailyHours: Double,
    val tasks: List<PlanTask>,
    val isSample: Boolean = false,
) {
    val completedCount: Int
        get() = tasks.count { it.isCompleted }

    val completionFraction: Float
        get() = if (tasks.isEmpty()) 0f else (completedCount.toFloat() / tasks.size.toFloat()).coerceIn(0f, 1f)

    val completionPercent: Int
        get() = kotlin.math.round(completionFraction * 100f).toInt().coerceIn(0, 100)
}

internal fun parseSubjectsList(subjectsRaw: String): List<String> =
    subjectsRaw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

internal fun formatHoursLabel(hours: Double): String {
    val rounded = kotlin.math.round(hours * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toInt()}h"
    } else {
        "${rounded}h"
    }
}

internal fun buildDistributedPlanTasks(
    subjects: List<String>,
    daysRemaining: Int,
    dailyHours: Double,
): List<PlanTask> {
    if (subjects.isEmpty()) return emptyList()

    val cappedDays = daysRemaining.coerceIn(1, MAX_PLAN_DAYS)
    val safeHours = dailyHours.coerceIn(0.5, 16.0)

    return if (subjects.size <= cappedDays) {
        val hoursLabel = formatHoursLabel(safeHours)
        (0 until cappedDays).map { dayIdx ->
            val subject = subjects[dayIdx % subjects.size]
            val passNumber = (dayIdx / subjects.size) + 1
            val focusVerb = when (passNumber) {
                1 -> "Core concepts & notes review"
                2 -> "Practice problems & recall drills"
                else -> "Timed past-paper & weak-spot drill"
            }
            PlanTask(
                id = "plan-day-${dayIdx + 1}",
                dayNumber = dayIdx + 1,
                subject = subject,
                description = "$subject — $focusVerb",
                hoursLabel = hoursLabel,
                isCompleted = false,
            )
        }
    } else {
        // More subjects than days: distribute all subjects evenly across the cappedDays buckets.
        val buckets = Array(cappedDays) { mutableListOf<String>() }
        subjects.forEachIndexed { idx, subject ->
            buckets[idx % cappedDays].add(subject)
        }
        buckets.flatMapIndexed { dayIdx, daySubjects ->
            val perSubjectHours = formatHoursLabel(safeHours / daySubjects.size.toDouble())
            daySubjects.mapIndexed { subIdx, subject ->
                PlanTask(
                    id = "plan-day-${dayIdx + 1}-s$subIdx",
                    dayNumber = dayIdx + 1,
                    subject = subject,
                    description = "$subject — Core review & practice questions",
                    hoursLabel = perSubjectHours,
                    isCompleted = false,
                )
            }
        }
    }
}

object DemoPlanData {
    fun samplePlan(todayIso: String = StudyDateUtils.todayIso()): RevisionPlan {
        val targetIso = StudyDateUtils.addDaysIso(todayIso, 7)
        val subjectsRaw = "Physics, Chemistry, Calculus, Biology"
        val dailyHours = 2.5
        val tasks = buildDistributedPlanTasks(
            subjects = parseSubjectsList(subjectsRaw),
            daysRemaining = 7,
            dailyHours = dailyHours,
        )
        return RevisionPlan(
            examName = "Science & Math Midterm",
            targetDateIso = targetIso,
            subjectsRaw = subjectsRaw,
            dailyHours = dailyHours,
            tasks = tasks,
            isSample = true,
        )
    }
}
