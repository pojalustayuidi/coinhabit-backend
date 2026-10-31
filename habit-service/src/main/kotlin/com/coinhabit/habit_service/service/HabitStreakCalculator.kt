
package com.coinhabit.habit_service.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object HabitStreakCalculator {

    // Обычный расчёт календарных дней.
    // Сохраняем для совместимости с предыдущими тестами.
    fun calculate(
        startDate: Instant,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val startDay = startDate
            .atZone(zoneId)
            .toLocalDate()

        return daysBetween(startDay, today)
    }

    // Расчёт текущей серии с учётом последнего срыва.
    fun calculateCurrentStreak(
        startDate: Instant,
        lastRelapseAt: Instant?,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val streakStartDay = if (lastRelapseAt != null) {
            // День срыва не засчитываем.
            // Новая серия начинается со следующего дня.
            lastRelapseAt
                .atZone(zoneId)
                .toLocalDate()
                .plusDays(1)
        } else {
            startDate
                .atZone(zoneId)
                .toLocalDate()
        }

        return daysBetween(streakStartDay, today)
    }

    // Общее число успешных дней:
    // предыдущие дни + текущая серия.
    fun calculateTotalDays(
        completedDaysBeforeCurrentStreak: Int,
        currentStreakDays: Int
    ): Int {
        require(completedDaysBeforeCurrentStreak >= 0)
        require(currentStreakDays >= 0)

        return Math.addExact(
            completedDaysBeforeCurrentStreak,
            currentStreakDays
        )
    }

    private fun daysBetween(
        startDay: LocalDate,
        today: LocalDate
    ): Int {
        val days = ChronoUnit.DAYS.between(startDay, today)

        return days
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()
    }
}
