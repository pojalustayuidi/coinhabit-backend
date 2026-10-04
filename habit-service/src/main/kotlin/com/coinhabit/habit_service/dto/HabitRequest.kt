package com.coinhabit.habit_service.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class HabitRequest(
    @field:NotBlank(message = "Название привычки не может быть пустым")
    val title: String,

    @field:NotNull(message = "Сумма не может быть пустой")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Сумма трат должна быть больше нуля")
    val monthlySpend: BigDecimal
)