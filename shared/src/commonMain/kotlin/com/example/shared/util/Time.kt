package com.example.shared.util

import kotlinx.datetime.*

data class WeekRange(val startMillis: Long, val endMillis: Long)

object Time {
    fun currentWeekRange(timeZone: TimeZone = TimeZone.currentSystemDefault()): WeekRange {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date

        // ISO week: Monday start
        val dayOfWeek = today.dayOfWeek.isoDayNumber // 1..7
        val monday = today.minus(DatePeriod(days = dayOfWeek - 1))
        val nextMonday = monday.plus(DatePeriod(days = 7))

        val startInstant: Instant = monday.atStartOfDayIn(timeZone)
        val endInstant: Instant = nextMonday.atStartOfDayIn(timeZone)

        val start = startInstant.toEpochMilliseconds()
        val end = endInstant.toEpochMilliseconds()
        return WeekRange(start, end)
    }
}