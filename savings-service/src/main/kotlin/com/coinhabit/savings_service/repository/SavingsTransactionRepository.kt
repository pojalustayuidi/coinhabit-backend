
package com.coinhabit.savings_service.repository

import com.coinhabit.savings_service.entity.SavingsTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
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

    @Query(
        """
        SELECT COALESCE(SUM(t.savedAmount), 0)
        FROM SavingsTransaction t
        WHERE t.userId = :userId
        """
    )
    fun getTotalSavedByUserId(
        @Param("userId") userId: UUID
    ): BigDecimal
}
