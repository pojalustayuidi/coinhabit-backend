
package com.coinhabit.habit_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "habits")
class Habit(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, precision = 10, scale = 2)
    var monthlySpend: BigDecimal,

    @Column(nullable = false)
    var startDate: Instant,

    @Column(nullable = false)
    var currentStreakDays: Int = 0,

    @Column(nullable = false)
    var status: String = "active",

    var lastRelapseAt: Instant? = null,

    @Column(
        name = "completed_days_before_current_streak",
        nullable = false
    )
    var completedDaysBeforeCurrentStreak: Int = 0
)
