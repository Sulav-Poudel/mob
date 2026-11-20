package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object DateUtils {
    private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")
    private const val MILLIS_PER_DAY = 86_400_000L

    fun utcDayStartMillis(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance(utcZone).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
        }
        return cal.timeInMillis
    }

    fun todayUtcDayStartMillis(): Long {
        val local = Calendar.getInstance()
        return utcDayStartMillis(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun currentYearMonth(): Pair<Int, Int> {
        val local = Calendar.getInstance()
        return local.get(Calendar.YEAR) to local.get(Calendar.MONTH)
    }

    fun utcYearMonthDay(millis: Long): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance(utcZone).apply { timeInMillis = millis }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun daysInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance(utcZone).apply {
            clear()
            set(year, monthZeroBased, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Returns 0 for Sunday, 1 for Monday, ..., 6 for Saturday */
    fun firstDayOfWeekOffset(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance(utcZone).apply {
            clear()
            set(year, monthZeroBased, 1)
        }
        return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance(utcZone).apply {
            clear()
            set(year, monthZeroBased, 1)
        }
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = utcZone }
        return fmt.format(cal.time)
    }

    fun formatUtcDateShort(millis: Long?): String {
        if (millis == null) return "Not set"
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US).apply { timeZone = utcZone }
        return fmt.format(Date(millis))
    }

    fun formatUtcDateCompact(millis: Long?): String {
        if (millis == null) return "—"
        val fmt = SimpleDateFormat("MMM d", Locale.US).apply { timeZone = utcZone }
        return fmt.format(Date(millis))
    }

    fun daysUntilUtcDate(millis: Long?): Int? {
        if (millis == null) return null
        val (y, m, d) = utcYearMonthDay(millis)
        val targetDay = utcDayStartMillis(y, m, d)
        val today = todayUtcDayStartMillis()
        return ((targetDay - today).toDouble() / MILLIS_PER_DAY).roundToInt()
    }

    fun countdownLabel(millis: Long?): String {
        val days = daysUntilUtcDate(millis) ?: return "No date"
        return when {
            days == 0 -> "Due today"
            days == 1 -> "Tomorrow"
            days > 1 -> "In ${days}d"
            days == -1 -> "1d overdue"
            else -> "${-days}d ago"
        }
    }
}
