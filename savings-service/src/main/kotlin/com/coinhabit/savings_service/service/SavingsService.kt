
package com.coinhabit.savings_service.service

import com.coinhabit.savings_service.entity.SavingsTransaction
import com.coinhabit.savings_service.repository.SavingsTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class SavingsService(
    private val savingsTransactionRepository: SavingsTransactionRepository
) {

    @Transactional
    fun recordSuccessfulDay(
        eventId: UUID,
        habitId: UUID,
        userId: UUID,
        successfulDay: LocalDate,
        monthlySpend: BigDecimal
    ): SavingsTransaction? {

        require(monthlySpend >= BigDecimal.ZERO) {
            "Ежемесячные расходы не могут быть отрицательными"
        }

        // Не обрабатываем повторно одно и то же событие.
        if (
            savingsTransactionRepository.existsByEventId(
                eventId
            )
        ) {
            return null
        }

        // Не начисляем экономию дважды за один
        // успешный день одной привычки.
        if (
            savingsTransactionRepository
                .existsByHabitIdAndSuccessfulDay(
                    habitId,
                    successfulDay
                )
        ) {
            return null
        }

        // Приводим сумму к двум знакам после запятой.
        val normalizedMonthlySpend = monthlySpend.setScale(
            2,
            RoundingMode.HALF_UP
        )

        // Временная модель: условные 30 дней в месяце.
        val savedAmount = normalizedMonthlySpend.divide(
            BigDecimal("30"),
            2,
            RoundingMode.HALF_UP
        )

        val transaction = SavingsTransaction(
            id = UUID.randomUUID(),
            eventId = eventId,
            habitId = habitId,
            userId = userId,
            successfulDay = successfulDay,
            monthlySpend = normalizedMonthlySpend,
            savedAmount = savedAmount
        )

        return savingsTransactionRepository.save(
            transaction
        )
    }
}
