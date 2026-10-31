
package com.coinhabit.savings_service.event

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class HabitDayPassedEvent(
    val eventId: UUID,
    val habitId: UUID,
    val userId: UUID,
    val successfulDay: LocalDate,
    val currentStreakDays: Int,
    val monthlySpend: BigDecimal,
    val timestamp: Long
)
