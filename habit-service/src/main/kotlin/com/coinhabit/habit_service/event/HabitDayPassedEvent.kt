package com.coinhabit.habit_service.event

import java.util.UUID

data class HabitDayPassedEvent(
    val habitId: UUID,
    val userId: UUID,
    val currentStreakDays: Int,
    val timestamp: Long = System.currentTimeMillis()
)