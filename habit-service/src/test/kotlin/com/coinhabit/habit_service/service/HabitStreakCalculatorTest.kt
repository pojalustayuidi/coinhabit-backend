
package com.coinhabit.habit_service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HabitStreakCalculatorTest {

    @Test
    fun `new habit starts with zero days`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse(
                "2026-10-31T12:00:00Z"
            ),
            today = LocalDate.of(2026, 10, 31),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(0, result)
    }

    @Test
    fun `streak increases after calendar midnight`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse(
                "2026-10-31T23:50:00Z"
            ),
            today = LocalDate.of(2026, 11, 1),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(1, result)
    }

    @Test
    fun `streak counts multiple calendar days`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse(
                "2026-10-26T10:00:00Z"
            ),
            today = LocalDate.of(2026, 10, 31),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(5, result)
    }

    @Test
    fun `future start date never gives negative streak`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse(
                "2026-11-05T10:00:00Z"
            ),
            today = LocalDate.of(2026, 10, 31),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(0, result)
    }

    @Test
    fun `calendar day respects user time zone`() {
        val result = HabitStreakCalculator.calculate(
            startDate = Instant.parse(
                "2026-10-31T22:50:00Z"
            ),
            today = LocalDate.of(2026, 11, 1),
            zoneId = ZoneId.of("Europe/Amsterdam")
        )

        assertEquals(1, result)
    }
}
