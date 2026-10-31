
package com.coinhabit.habit_service.event

import java.time.LocalDate
import java.util.UUID

data class HabitDayPassedEvent(
    val eventId: UUID,
    val habitId: UUID,
    val userId: UUID,
    val successfulDay: LocalDate,
    val currentStreakDays: Int,
    val timestamp: Long = System.currentTimeMillis()
)
