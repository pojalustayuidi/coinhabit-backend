
package com.coinhabit.habit_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "habit_day_outbox",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_habit_successful_day",
            columnNames = ["habit_id", "successful_day"]
        )
    ]
)
class HabitDayOutbox(

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "habit_id", nullable = false)
    val habitId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "successful_day", nullable = false)
    val successfulDay: LocalDate,

    @Column(name = "current_streak_days", nullable = false)
    val currentStreakDays: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var published: Boolean = false
)
