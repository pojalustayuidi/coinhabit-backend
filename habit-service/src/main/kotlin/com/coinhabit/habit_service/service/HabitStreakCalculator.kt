
package com.coinhabit.habit_service.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object HabitStreakCalculator {

    fun calculate(
        startDate: Instant,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val startDay = startDate
            .atZone(zoneId)
            .toLocalDate()

        return ChronoUnit.DAYS
            .between(startDay, today)
            .coerceAtLeast(0)
            .toInt()
    }
}
