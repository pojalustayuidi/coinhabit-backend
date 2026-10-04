package com.coinhabit.habit_service.event

import java.util.UUID

data class HabitRelapsedEvent(
    val habitId: UUID,
    val userId: UUID,
    val timestamp: Long = System.currentTimeMillis()
)