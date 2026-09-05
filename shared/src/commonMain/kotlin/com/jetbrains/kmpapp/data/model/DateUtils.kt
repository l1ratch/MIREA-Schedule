package com.jetbrains.kmpapp.data.model

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object DateUtils {

    fun today(): LocalDate {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return now.date
    }

    fun getWeekDates(anchorDate: LocalDate): List<LocalDate> {
        val dayOfWeekIndex = when (anchorDate.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
        val monday = anchorDate.minus(DatePeriod(days = dayOfWeekIndex))
        return (0..6).map { monday.plus(DatePeriod(days = it)) }
    }

    fun getWeekInfo(date: LocalDate): SemesterWeekInfo {
        val monthNum = date.month.ordinal + 1
        val semesterStart = if (monthNum in 2..8) {
            LocalDate(date.year, 2, 9)
        } else {
            val startYear = if (monthNum == 1) date.year - 1 else date.year
            LocalDate(startYear, 9, 1)
        }

        val daysBetween = semesterStart.daysUntil(date)
        val weekNumber = if (daysBetween >= 0) (daysBetween / 7) + 1 else 1
        val isEven = weekNumber % 2 == 0

        return SemesterWeekInfo(
            weekNumber = weekNumber.coerceAtLeast(1),
            isEven = isEven
        )
    }

    fun formatDayOfWeekShort(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Пн"
            DayOfWeek.TUESDAY -> "Вт"
            DayOfWeek.WEDNESDAY -> "Ср"
            DayOfWeek.THURSDAY -> "Чт"
            DayOfWeek.FRIDAY -> "Пт"
            DayOfWeek.SATURDAY -> "Сб"
            DayOfWeek.SUNDAY -> "Вс"
        }
    }

    fun formatMonthRu(month: Month): String {
        return when (month) {
            Month.JANUARY -> "января"
            Month.FEBRUARY -> "февраля"
            Month.MARCH -> "марта"
            Month.APRIL -> "апреля"
            Month.MAY -> "мая"
            Month.JUNE -> "июня"
            Month.JULY -> "июля"
            Month.AUGUST -> "августа"
            Month.SEPTEMBER -> "сентября"
            Month.OCTOBER -> "октября"
            Month.NOVEMBER -> "ноября"
            Month.DECEMBER -> "декабря"
        }
    }
}
