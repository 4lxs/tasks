package com.example.todo.util

import androidx.core.text.util.LocalePreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

data class Week(private val day: LocalDate) {
    val firstDay: LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastDay: LocalDate = day.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    fun plus(count: Int) = Week(day.plusWeeks(count.toLong()))

    fun forEachDay(action: (LocalDate) -> Unit) {
        for (i in 0L until 7L) {
            action(firstDay.plusDays(i))
        }
    }

    operator fun minus(other: Week): Long = other.firstDay.until(firstDay, ChronoUnit.DAYS) / 7

    companion object {
        fun current() = Week(LocalDate.now())
        fun of(focusedDate: LocalDate) = Week(focusedDate)
    }

}