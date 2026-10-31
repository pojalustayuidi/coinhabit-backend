
package com.coinhabit.habit_service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HabitStreakCalculatorTest {

    private val utc = ZoneOffset.UTC

    @Test
    fun `new habit starts with zero days`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse("2026-10-31T12:00:00Z"),
            today = LocalDate.of(2026, 10, 31),
            zoneId = utc
        )

        assertEquals(0, result)
    }

    @Test
    fun `new calendar day increases streak`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse("2026-10-31T23:50:00Z"),
            today = LocalDate.of(2026, 11, 1),
            zoneId = utc
        )

        assertEquals(1, result)
    }

    @Test
    fun `five calendar days are counted`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse("2026-10-26T10:00:00Z"),
            today = LocalDate.of(2026, 10, 31),
            zoneId = utc
        )

        assertEquals(5, result)
    }

    @Test
    fun `future start gives zero days`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse("2026-11-05T10:00:00Z"),
            today = LocalDate.of(2026, 10, 31),
            zoneId = utc
        )

        assertEquals(0, result)
    }

    @Test
    fun `user time zone is respected`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse("2026-10-31T22:50:00Z"),
            today = LocalDate.of(2026, 11, 1),
            zoneId = ZoneId.of("Europe/Amsterdam")
        )

        assertEquals(1, result)
    }

    @Test
    fun `relapse day is not counted`() {
        val result = HabitStreakCalculator.calculateCurrentStreak(
            startDate = Instant.parse("2026-10-26T10:00:00Z"),
            lastRelapseAt = Instant.parse("2026-10-31T08:00:00Z"),
            today = LocalDate.of(2026, 10, 31),
            zoneId = utc
        )

        assertEquals(0, result)
    }

    @Test
    fun `day after relapse is not yet completed`() {
        val result = HabitStreakCalculator.calculateCurrentStreak(
            startDate = Instant.parse("2026-10-26T10:00:00Z"),
            lastRelapseAt = Instant.parse("2026-10-31T08:00:00Z"),
            today = LocalDate.of(2026, 11, 1),
            zoneId = utc
        )

        assertEquals(0, result)
    }

    @Test
    fun `completed day after relapse increases streak`() {
        val result = HabitStreakCalculator.calculateCurrentStreak(
            startDate = Instant.parse("2026-10-26T10:00:00Z"),
            lastRelapseAt = Instant.parse("2026-10-31T08:00:00Z"),
            today = LocalDate.of(2026, 11, 2),
            zoneId = utc
        )

        assertEquals(1, result)
    }

    @Test
    fun `previously earned days are preserved`() {
        val result = HabitStreakCalculator.calculateTotalDays(
            completedDaysBeforeCurrentStreak = 5,
            currentStreakDays = 0
        )

        assertEquals(5, result)
    }

    @Test
    fun `new streak is added to previous successful days`() {
        val result = HabitStreakCalculator.calculateTotalDays(
            completedDaysBeforeCurrentStreak = 5,
            currentStreakDays = 3
        )

        assertEquals(8, result)
    }
}
