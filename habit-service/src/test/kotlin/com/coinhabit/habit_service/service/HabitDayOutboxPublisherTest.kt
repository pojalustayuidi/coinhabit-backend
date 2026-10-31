
package com.coinhabit.habit_service.service

import com.coinhabit.habit_service.entity.HabitDayOutbox
import com.coinhabit.habit_service.event.HabitDayPassedEvent
import com.coinhabit.habit_service.repository.HabitDayOutboxRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.time.LocalDate
import java.util.UUID

class HabitDayOutboxPublisherTest {

    private val repository = mock(
        HabitDayOutboxRepository::class.java
    )

    private val rabbitTemplate = mock(
        RabbitTemplate::class.java
    )

    private val publisher = HabitDayOutboxPublisher(
        repository,
        rabbitTemplate
    )

    private fun createRecord() = HabitDayOutbox(
        id = UUID.fromString(
            "00000000-0000-4000-8000-000000000301"
        ),
        habitId = UUID.fromString(
            "00000000-0000-4000-8000-000000000302"
        ),
        userId = UUID.fromString(
            "00000000-0000-4000-8000-000000000303"
        ),
        successfulDay = LocalDate.of(
            2026, 10, 30
        ),
        currentStreakDays = 1,
        published = false
    )

    private fun simulateBrokerConfirm(ack: Boolean) {

        doAnswer { invocation ->

            val correlationData =
                invocation.getArgument<CorrelationData>(3)

            correlationData.future.complete(
                CorrelationData.Confirm(
                    ack,
                    if (ack) null else "Broker rejected publication"
                )
            )

            null
        }.`when`(rabbitTemplate).convertAndSend(
            eq("habit.events"),
            eq("habit.day.passed"),
            any(HabitDayPassedEvent::class.java),
            any(CorrelationData::class.java)
        )
    }

    @Test
    fun `confirmed event becomes published`() {

        val record = createRecord()

        `when`(
            repository
                .findTop100ByPublishedFalseOrderBySuccessfulDayAsc()
        ).thenReturn(listOf(record))

        simulateBrokerConfirm(ack = true)

        publisher.publishPendingEvents()

        assertTrue(record.published)

        verify(repository).save(record)
    }

    @Test
    fun `rejected event remains unpublished`() {

        val record = createRecord()

        `when`(
            repository
                .findTop100ByPublishedFalseOrderBySuccessfulDayAsc()
        ).thenReturn(listOf(record))

        simulateBrokerConfirm(ack = false)

        publisher.publishPendingEvents()

        assertFalse(record.published)

        verify(repository, never()).save(
            any(HabitDayOutbox::class.java)
        )
    }

    @Test
    fun `empty outbox sends no events`() {

        `when`(
            repository
                .findTop100ByPublishedFalseOrderBySuccessfulDayAsc()
        ).thenReturn(emptyList())

        publisher.publishPendingEvents()

        verifyNoInteractions(rabbitTemplate)

        assertEquals(
            0,
            repository
                .findTop100ByPublishedFalseOrderBySuccessfulDayAsc()
                .size
        )
    }
}
