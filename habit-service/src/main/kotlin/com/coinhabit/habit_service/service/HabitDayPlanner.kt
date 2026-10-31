
package com.coinhabit.habit_service.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class PlannedHabitDay(
    val successfulDay: LocalDate,
    val currentStreakDays: Int
)

object HabitDayPlanner {

    fun planCompletedDays(
        startDate: Instant,
        lastRelapseAt: Instant?,
        today: LocalDate,
        zoneId: ZoneId,
        lastRecordedDay: LocalDate?
    ): List<PlannedHabitDay> {

        // Определяем первый день текущей серии.
        val streakStartDay = if (lastRelapseAt != null) {
            // День срыва не засчитывается.
            lastRelapseAt
                .atZone(zoneId)
                .toLocalDate()
                .plusDays(1)
        } else {
            startDate
                .atZone(zoneId)
                .toLocalDate()
        }

        // Если часть дней уже записана в базу,
        // не формируем их повторно.
        val firstUnrecordedDay = if (
            lastRecordedDay != null &&
            !lastRecordedDay.isBefore(streakStartDay)
        ) {
            lastRecordedDay.plusDays(1)
        } else {
            streakStartDay
        }

        // Сегодня ещё не завершилось.
        if (!firstUnrecordedDay.isBefore(today)) {
            return emptyList()
        }

        val numberOfDays = ChronoUnit.DAYS.between(
            firstUnrecordedDay,
            today
        )

        return (0L until numberOfDays).map { offset ->

            val day = firstUnrecordedDay.plusDays(offset)

            val streakDays = ChronoUnit.DAYS.between(
                streakStartDay,
                day
            ) + 1

            require(streakDays <= Int.MAX_VALUE) {
                "Слишком большая продолжительность серии"
            }

            PlannedHabitDay(
                successfulDay = day,
                currentStreakDays = streakDays.toInt()
            )
        }
    }
}
