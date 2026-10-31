
package com.coinhabit.savings_service.service

import com.coinhabit.savings_service.repository.SavingsTransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SavingsSummaryServiceTest {

    private val repository = mock(
        SavingsTransactionRepository::class.java
    )

    private val service = SavingsSummaryService(repository)

    private val userId = UUID.fromString(
        "00000000-0000-4000-8000-000000000901"
    )

    @Test
    fun `returns total confirmed savings`() {

        // Например, пользователь сэкономил:
        // 1200 рублей на энергетиках
        // и 2800 рублей на сигаретах.
        `when`(
            repository.getTotalSavedByUserId(userId)
        ).thenReturn(BigDecimal("4000.00"))

        val result = service.getConfirmedTotal(userId)

        assertEquals(
            BigDecimal("4000.00"),
            result
        )

        verify(repository).getTotalSavedByUserId(userId)
    }

    @Test
    fun `returns zero when user has no savings`() {

        `when`(
            repository.getTotalSavedByUserId(userId)
        ).thenReturn(BigDecimal.ZERO)

        val result = service.getConfirmedTotal(userId)

        assertEquals(
            BigDecimal("0.00"),
            result
        )

        verify(repository).getTotalSavedByUserId(userId)
    }

    @Test
    fun `normalizes total to two decimal places`() {

        `when`(
            repository.getTotalSavedByUserId(userId)
        ).thenReturn(BigDecimal("1250.0"))

        val result = service.getConfirmedTotal(userId)

        assertEquals(
            BigDecimal("1250.00"),
            result
        )

        assertEquals(2, result.scale())
    }

    @Test
    fun `requests savings for correct user`() {

        val anotherUserId = UUID.fromString(
            "00000000-0000-4000-8000-000000000902"
        )

        `when`(
            repository.getTotalSavedByUserId(anotherUserId)
        ).thenReturn(BigDecimal("750.00"))

        val result = service.getConfirmedTotal(
            anotherUserId
        )

        assertEquals(
            BigDecimal("750.00"),
            result
        )

        verify(repository).getTotalSavedByUserId(
            anotherUserId
        )
    }
}
