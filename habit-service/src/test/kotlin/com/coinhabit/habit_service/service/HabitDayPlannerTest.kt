
package com.coinhabit.habit_service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HabitDayPlannerTest {

    private val utc = ZoneOffset.UTC

    @Test
    fun `new habit has no completed days`() {
        val result = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-31T12:00:00Z"
            ),
            lastRelapseAt = null,
            today = LocalDate.of(2026, 10, 31),
            zoneId = utc,
            lastRecordedDay = null
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `next calendar day creates one record`() {
        val result = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-31T23:50:00Z"
            ),
            lastRelapseAt = null,
            today = LocalDate.of(2026, 11, 1),
            zoneId = utc,
            lastRecordedDay = null
        )

        assertEquals(1, result.size)
        assertEquals(
            LocalDate.of(2026, 10, 31),
            result[0].successfulDay
        )
        assertEquals(
            1,
            result[0].currentStreakDays
        )
    }

    @Test
    fun `missed days are restored`() {
        val result = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-29T10:00:00Z"
            ),
            lastRelapseAt = null,
            today = LocalDate.of(2026, 11, 2),
            zoneId = utc,
            lastRecordedDay = null
        )

        assertEquals(4, result.size)
        assertEquals(
            LocalDate.of(2026, 11, 1),
            result.last().successfulDay
        )
        assertEquals(
            4,
            result.last().currentStreakDays
        )
    }

    @Test
    fun `already recorded days are not created again`() {
        val firstRun = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-29T10:00:00Z"
            ),
            lastRelapseAt = null,
            today = LocalDate.of(2026, 11, 2),
            zoneId = utc,
            lastRecordedDay = null
        )

        val secondRun = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-29T10:00:00Z"
            ),
            lastRelapseAt = null,
            today = LocalDate.of(2026, 11, 2),
            zoneId = utc,
            lastRecordedDay =
                firstRun.last().successfulDay
        )

        assertEquals(4, firstRun.size)
        assertTrue(secondRun.isEmpty())
    }

    @Test
    fun `relapse day is skipped`() {
        val result = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-26T10:00:00Z"
            ),
            lastRelapseAt = Instant.parse(
                "2026-10-31T08:00:00Z"
            ),
            today = LocalDate.of(2026, 11, 2),
            zoneId = utc,
            lastRecordedDay = null
        )

        assertEquals(1, result.size)
        assertEquals(
            LocalDate.of(2026, 11, 1),
            result[0].successfulDay
        )
        assertEquals(
            1,
            result[0].currentStreakDays
        )
    }

    @Test
    fun `time zone is respected`() {
        // В Амстердаме 1 ноября 2026 года UTC+1.
        // 31 октября 23:50 UTC = 1 ноября 00:50.
        val result = HabitDayPlanner.planCompletedDays(
            startDate = Instant.parse(
                "2026-10-31T23:50:00Z"
            ),
            lastRelapseAt = null,
            today = LocalDate.of(2026, 11, 2),
            zoneId = ZoneId.of("Europe/Amsterdam"),
            lastRecordedDay = null
        )

        assertEquals(1, result.size)
        assertEquals(
            LocalDate.of(2026, 11, 1),
            result[0].successfulDay
        )
        assertEquals(
            1,
            result[0].currentStreakDays
        )
    }
}
