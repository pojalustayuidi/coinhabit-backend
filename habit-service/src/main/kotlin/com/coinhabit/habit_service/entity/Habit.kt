package com.coinhabit.habit_service.entity

import jakarta.persistence.*
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
    val title: String,

    @Column(nullable = false, precision = 10, scale = 2)
    val monthlySpend: BigDecimal,

    @Column(nullable = false)
    val startDate: Instant,

    @Column(nullable = false)
    var currentStreakDays: Int = 0,

    @Column(nullable = false)
    var status: String = "active",

    var lastRelapseAt: Instant? = null
)