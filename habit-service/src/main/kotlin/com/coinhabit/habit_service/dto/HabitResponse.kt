package com.coinhabit.habit_service.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class HabitResponse(
    val id: UUID,
    val title: String,
    val monthlySpend: BigDecimal,
    val startDate: Instant,
    val currentStreakDays: Int,
    val status: String,
    val lastRelapseAt: Instant?,
    val savedAmount: BigDecimal // Динамически высчитанная экономия
)