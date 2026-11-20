package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object StudyDateUtils {
    private const val ISO_PATTERN = "yyyy-MM-dd"

    private fun newIsoFormatter(): SimpleDateFormat =
        SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getDefault()
        }

    fun todayIso(nowMillis: Long = System.currentTimeMillis()): String {
        return newIsoFormatter().format(nowMillis)
    }

    fun yesterdayIso(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return newIsoFormatter().format(cal.time)
    }

    fun addDaysIso(baseIso: String, daysToAdd: Int): String {
        val cal = parseToMidnightCalendar(baseIso) ?: Calendar.getInstance().apply {
            setToMidnight()
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return newIsoFormatter().format(cal.time)
    }

    fun isValidIsoDate(dateStr: String): Boolean = parseToMidnightCalendar(dateStr.trim()) != null

    fun daysBetweenIso(fromIso: String, toIso: String): Int {
        val fromCal = parseToMidnightCalendar(fromIso) ?: return 0
        val toCal = parseToMidnightCalendar(toIso) ?: return 0
        val diffMillis = toCal.timeInMillis - fromCal.timeInMillis
        val days = (diffMillis.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)).roundToInt()
        return days.coerceAtLeast(0)
    }

    fun utcPickerMillisToLocalIso(utcMillis: Long): String {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance().apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0,
            )
            set(Calendar.MILLISECOND, 0)
        }
        return newIsoFormatter().format(localCal.time)
    }

    private fun parseToMidnightCalendar(dateStr: String): Calendar? {
        return try {
            val parsed = newIsoFormatter().parse(dateStr.trim()) ?: return null
            Calendar.getInstance().apply {
                time = parsed
                setToMidnight()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun Calendar.setToMidnight() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
