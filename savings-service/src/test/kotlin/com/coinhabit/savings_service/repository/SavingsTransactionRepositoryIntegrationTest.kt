
package com.coinhabit.savings_service.repository

import com.coinhabit.savings_service.entity.SavingsTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Transactional
class SavingsTransactionRepositoryIntegrationTest @Autowired constructor(
    private val repository: SavingsTransactionRepository
) {

    @Test
    fun `sums savings from multiple habits for one user`() {

        val userId = UUID.randomUUID()
        val anotherUserId = UUID.randomUUID()

        repository.saveAll(
            listOf(
                createTransaction(
                    userId = userId,
                    monthlySpend = BigDecimal("3000.00"),
                    savedAmount = BigDecimal("100.00")
                ),
                createTransaction(
                    userId = userId,
                    monthlySpend = BigDecimal("1500.00"),
                    savedAmount = BigDecimal("50.00")
                ),
                createTransaction(
                    userId = anotherUserId,
                    monthlySpend = BigDecimal("900.00"),
                    savedAmount = BigDecimal("30.00")
                )
            )
        )

        repository.flush()

        val totalSaved = repository.getTotalSavedByUserId(
            userId
        )

        // Две привычки одного пользователя:
        // 100.00 + 50.00 = 150.00.
        // Начисление другого пользователя не учитывается.
        assertEquals(
            BigDecimal("150.00"),
            totalSaved.setScale(2)
        )
    }

    @Test
    fun `returns zero for user without transactions`() {

        val userId = UUID.randomUUID()

        val totalSaved = repository.getTotalSavedByUserId(
            userId
        )

        assertEquals(
            BigDecimal("0.00"),
            totalSaved.setScale(2)
        )
    }

    private fun createTransaction(
        userId: UUID,
        monthlySpend: BigDecimal,
        savedAmount: BigDecimal
    ): SavingsTransaction {

        return SavingsTransaction(
            id = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            userId = userId,
            successfulDay = LocalDate.of(2026, 10, 30),
            monthlySpend = monthlySpend,
            savedAmount = savedAmount
        )
    }
}
