
package com.coinhabit.habit_service.service

import com.coinhabit.habit_service.dto.HabitRequest
import com.coinhabit.habit_service.entity.Habit
import com.coinhabit.habit_service.repository.HabitRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.NoSuchElementException
import java.util.Optional
import java.util.UUID

class HabitManagementServiceTest {

    private val ownerId = UUID.randomUUID()
    private val anotherUserId = UUID.randomUUID()
    private val habitId = UUID.randomUUID()

    private val repository = mock(
        HabitRepository::class.java
    )

    private val service = HabitService(repository)

    private fun createHabit(
        status: String = "active"
    ) = Habit(
        id = habitId,
        userId = ownerId,
        title = "Отказ от сигарет",
        monthlySpend = BigDecimal("3000.00"),
        startDate = Instant.parse("2026-10-20T12:00:00Z"),
        currentStreakDays = 3,
        status = status,
        completedDaysBeforeCurrentStreak = 2
    )

    private fun stubHabit(habit: Habit) {
        `when`(repository.findById(habitId))
            .thenReturn(Optional.of(habit))
    }

    private fun stubMissingHabit() {
        `when`(repository.findById(habitId))
            .thenReturn(Optional.empty())
    }

    @Test
    fun `owner can update habit`() {
        val habit = createHabit()
        stubHabit(habit)

        val originalStartDate = habit.startDate

        service.updateHabit(
            habitId,
            ownerId,
            HabitRequest(
                title = "  Без сигарет  ",
                monthlySpend = BigDecimal("5000.00")
            )
        )

        assertEquals("Без сигарет", habit.title)
        assertEquals(
            BigDecimal("5000.00"),
            habit.monthlySpend
        )
        assertEquals(originalStartDate, habit.startDate)
        assertEquals(3, habit.currentStreakDays)
        assertEquals(
            2,
            habit.completedDaysBeforeCurrentStreak
        )
        assertEquals("active", habit.status)

        verify(repository).save(habit)
    }

    @Test
    fun `another user cannot update habit`() {
        val habit = createHabit()
        stubHabit(habit)

        assertThrows(SecurityException::class.java) {
            service.updateHabit(
                habitId,
                anotherUserId,
                HabitRequest(
                    "Чужая привычка",
                    BigDecimal("9000.00")
                )
            )
        }

        assertEquals("Отказ от сигарет", habit.title)
        verify(repository, never()).save(habit)
    }

    @Test
    fun `missing habit cannot be updated`() {
        stubMissingHabit()

        val exception = assertThrows(
            NoSuchElementException::class.java
        ) {
            service.updateHabit(
                habitId,
                ownerId,
                HabitRequest(
                    "Без сигарет",
                    BigDecimal("5000.00")
                )
            )
        }

        assertEquals(
            "Привычка не найдена",
            exception.message
        )
    }

    @Test
    fun `empty title is rejected`() {
        val habit = createHabit()
        stubHabit(habit)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateHabit(
                habitId,
                ownerId,
                HabitRequest(
                    "   ",
                    BigDecimal("5000.00")
                )
            )
        }

        verify(repository, never()).save(habit)
    }

    @Test
    fun `negative monthly spend is rejected`() {
        val habit = createHabit()
        stubHabit(habit)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateHabit(
                habitId,
                ownerId,
                HabitRequest(
                    "Без сигарет",
                    BigDecimal("-100.00")
                )
            )
        }

        verify(repository, never()).save(habit)
    }

    @Test
    fun `monthly spend is rounded to two decimals`() {
        val habit = createHabit()
        stubHabit(habit)

        service.updateHabit(
            habitId,
            ownerId,
            HabitRequest(
                "Без сигарет",
                BigDecimal("5500.555")
            )
        )

        assertEquals(
            BigDecimal("5500.56"),
            habit.monthlySpend
        )

        verify(repository).save(habit)
    }

    @Test
    fun `archived habit cannot be edited`() {
        val habit = createHabit(status = "archived")
        stubHabit(habit)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateHabit(
                habitId,
                ownerId,
                HabitRequest(
                    "Новое название",
                    BigDecimal("6000.00")
                )
            )
        }

        verify(repository, never()).save(habit)
    }

    @Test
    fun `owner can archive habit`() {
        val habit = createHabit()
        stubHabit(habit)

        val originalStartDate = habit.startDate

        service.archiveHabit(habitId, ownerId)

        assertEquals("archived", habit.status)
        assertEquals(originalStartDate, habit.startDate)
        assertEquals(
            2,
            habit.completedDaysBeforeCurrentStreak
        )

        val expectedStreak =
            HabitStreakCalculator.calculateCurrentStreak(
                startDate = habit.startDate,
                lastRelapseAt = habit.lastRelapseAt,
                today = LocalDate.now(ZoneOffset.UTC),
                zoneId = ZoneOffset.UTC
            )

        assertEquals(
            expectedStreak,
            habit.currentStreakDays
        )

        verify(repository).save(habit)
    }

    @Test
    fun `archiving twice does not change habit again`() {
        val habit = createHabit(status = "archived")
        stubHabit(habit)

        service.archiveHabit(habitId, ownerId)

        assertEquals("archived", habit.status)
        assertEquals(3, habit.currentStreakDays)

        verify(repository, never()).save(habit)
    }

    @Test
    fun `another user cannot archive habit`() {
        val habit = createHabit()
        stubHabit(habit)

        assertThrows(SecurityException::class.java) {
            service.archiveHabit(
                habitId,
                anotherUserId
            )
        }

        assertEquals("active", habit.status)
        verify(repository, never()).save(habit)
    }

    @Test
    fun `missing habit cannot be archived`() {
        stubMissingHabit()

        val exception = assertThrows(
            NoSuchElementException::class.java
        ) {
            service.archiveHabit(
                habitId,
                ownerId
            )
        }

        assertTrue(
            exception.message?.contains(
                "Привычка не найдена"
            ) == true
        )
    }

    @Test
    fun `archived habit cannot be relapsed`() {
        val habit = createHabit(status = "archived")
        stubHabit(habit)

        assertThrows(IllegalArgumentException::class.java) {
            service.relapseHabit(
                habitId,
                ownerId
            )
        }

        verify(repository, never()).save(habit)
    }

    @Test
    fun `missing habit cannot be relapsed`() {
        stubMissingHabit()

        val exception = assertThrows(
            NoSuchElementException::class.java
        ) {
            service.relapseHabit(
                habitId,
                ownerId
            )
        }

        assertEquals(
            "Привычка не найдена",
            exception.message
        )
    }
}
