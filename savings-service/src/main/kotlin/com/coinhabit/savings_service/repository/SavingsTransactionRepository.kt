
package com.coinhabit.savings_service.repository

import com.coinhabit.savings_service.entity.SavingsTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface SavingsTransactionRepository :
    JpaRepository<SavingsTransaction, UUID> {

    fun existsByEventId(
        eventId: UUID
    ): Boolean

    fun existsByHabitIdAndSuccessfulDay(
        habitId: UUID,
        successfulDay: LocalDate
    ): Boolean
}
