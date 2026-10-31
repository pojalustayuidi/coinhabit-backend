
package com.coinhabit.savings_service.service

import com.coinhabit.savings_service.entity.SavingsTransaction
import com.coinhabit.savings_service.repository.SavingsTransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class SavingsServiceTest {

    private val repository = mock(
        SavingsTransactionRepository::class.java
    )

    private val service = SavingsService(repository)

    private val eventId = UUID.fromString(
        "00000000-0000-4000-8000-000000000501"
    )

    private val habitId = UUID.fromString(
        "00000000-0000-4000-8000-000000000502"
    )

    private val userId = UUID.fromString(
        "00000000-0000-4000-8000-000000000503"
    )

    private val successfulDay = LocalDate.of(
        2026,
        10,
        30
    )

    private fun mockSavingTransaction() {
        `when`(
            repository.save(
                any(SavingsTransaction::class.java)
            )
        ).thenAnswer { invocation ->
            invocation.getArgument<SavingsTransaction>(0)
        }
    }

    private fun recordDay(
        monthlySpend: BigDecimal,
        eventId: UUID = this.eventId
    ): SavingsTransaction? {
        return service.recordSuccessfulDay(
            eventId = eventId,
            habitId = habitId,
            userId = userId,
            successfulDay = successfulDay,
            monthlySpend = monthlySpend
        )
    }

    @Test
    fun `monthly spend of 3000 saves 100 per day`() {
        mockSavingTransaction()

        val transaction = recordDay(
            BigDecimal("3000.00")
        )

        assertNotNull(transaction)

        assertEquals(
            BigDecimal("100.00"),
            transaction!!.savedAmount
        )

        assertEquals(
            BigDecimal("3000.00"),
            transaction.monthlySpend
        )

        assertEquals(eventId, transaction.eventId)
        assertEquals(habitId, transaction.habitId)
        assertEquals(userId, transaction.userId)
        assertEquals(
            successfulDay,
            transaction.successfulDay
        )

        verify(repository).save(transaction)
    }

    @Test
    fun `daily savings are rounded to two decimals`() {
        mockSavingTransaction()

        val transaction = recordDay(
            BigDecimal("1000.00")
        )

        assertNotNull(transaction)

        assertEquals(
            BigDecimal("33.33"),
            transaction!!.savedAmount
        )
    }

    @Test
    fun `duplicate event is not saved again`() {
        `when`(
            repository.existsByEventId(eventId)
        ).thenReturn(true)

        val result = recordDay(
            BigDecimal("3000.00")
        )

        assertNull(result)

        verify(repository, never()).save(
            any(SavingsTransaction::class.java)
        )
    }

    @Test
    fun `duplicate habit day is not saved again`() {
        `when`(
            repository.existsByHabitIdAndSuccessfulDay(
                habitId,
                successfulDay
            )
        ).thenReturn(true)

        val result = recordDay(
            BigDecimal("3000.00")
        )

        assertNull(result)

        verify(repository, never()).save(
            any(SavingsTransaction::class.java)
        )
    }

    @Test
    fun `negative monthly spend is rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            recordDay(
                BigDecimal("-500.00")
            )
        }

        verifyNoInteractions(repository)
    }

    @Test
    fun `zero monthly spend creates zero savings`() {
        mockSavingTransaction()

        val transaction = recordDay(
            BigDecimal("0.00")
        )

        assertNotNull(transaction)

        assertEquals(
            BigDecimal("0.00"),
            transaction!!.savedAmount
        )
    }
}
