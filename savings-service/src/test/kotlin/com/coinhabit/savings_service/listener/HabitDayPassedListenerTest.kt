
package com.coinhabit.savings_service.listener

import com.coinhabit.savings_service.entity.SavingsTransaction
import com.coinhabit.savings_service.event.HabitDayPassedEvent
import com.coinhabit.savings_service.repository.SavingsTransactionRepository
import com.coinhabit.savings_service.service.SavingsService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class HabitDayPassedListenerTest {

    private val savingsService = mock(
        SavingsService::class.java
    )

    private val repository = mock(
        SavingsTransactionRepository::class.java
    )

    private val listener = HabitDayPassedListener(
        savingsService,
        repository
    )

    private val event = HabitDayPassedEvent(
        eventId = UUID.fromString(
            "00000000-0000-4000-8000-000000000801"
        ),
        habitId = UUID.fromString(
            "00000000-0000-4000-8000-000000000802"
        ),
        userId = UUID.fromString(
            "00000000-0000-4000-8000-000000000803"
        ),
        successfulDay = LocalDate.of(2026, 10, 30),
        currentStreakDays = 1,
        monthlySpend = BigDecimal("3000.00"),
        timestamp = 1793446692171L
    )

    private fun recordDay(): SavingsTransaction? {
        return savingsService.recordSuccessfulDay(
            eventId = event.eventId,
            habitId = event.habitId,
            userId = event.userId,
            successfulDay = event.successfulDay,
            monthlySpend = event.monthlySpend
        )
    }

    private fun createTransaction() = SavingsTransaction(
        id = UUID.randomUUID(),
        eventId = event.eventId,
        habitId = event.habitId,
        userId = event.userId,
        successfulDay = event.successfulDay,
        monthlySpend = event.monthlySpend,
        savedAmount = BigDecimal("100.00")
    )

    private fun simulateDatabaseConflict() {
        `when`(recordDay()).thenThrow(
            DataIntegrityViolationException(
                "Duplicate database key"
            )
        )
    }

    @Test
    fun `successful event creates savings transaction`() {
        val transaction = createTransaction()

        `when`(recordDay()).thenReturn(transaction)

        listener.handleHabitDayPassed(event)

        verify(savingsService).recordSuccessfulDay(
            event.eventId,
            event.habitId,
            event.userId,
            event.successfulDay,
            event.monthlySpend
        )

        verifyNoInteractions(repository)
    }

    @Test
    fun `already processed event is skipped`() {
        `when`(recordDay()).thenReturn(null)

        listener.handleHabitDayPassed(event)

        verify(savingsService).recordSuccessfulDay(
            event.eventId,
            event.habitId,
            event.userId,
            event.successfulDay,
            event.monthlySpend
        )

        verifyNoInteractions(repository)
    }

    @Test
    fun `database conflict with existing event is skipped`() {
        simulateDatabaseConflict()

        `when`(
            repository.existsByEventId(event.eventId)
        ).thenReturn(true)

        listener.handleHabitDayPassed(event)

        verify(repository).existsByEventId(
            event.eventId
        )
    }

    @Test
    fun `database conflict with existing habit day is skipped`() {
        simulateDatabaseConflict()

        `when`(
            repository.existsByHabitIdAndSuccessfulDay(
                event.habitId,
                event.successfulDay
            )
        ).thenReturn(true)

        listener.handleHabitDayPassed(event)

        verify(repository).existsByHabitIdAndSuccessfulDay(
            event.habitId,
            event.successfulDay
        )
    }

    @Test
    fun `unrelated database error is not hidden`() {
        simulateDatabaseConflict()

        assertThrows(
            DataIntegrityViolationException::class.java
        ) {
            listener.handleHabitDayPassed(event)
        }

        verify(repository).existsByEventId(
            event.eventId
        )

        verify(repository).existsByHabitIdAndSuccessfulDay(
            event.habitId,
            event.successfulDay
            )
        }
}
