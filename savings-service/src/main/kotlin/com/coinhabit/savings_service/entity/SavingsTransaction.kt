
package com.coinhabit.savings_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "savings_transactions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_savings_habit_day",
            columnNames = ["habit_id", "successful_day"]
        )
    ]
)
class SavingsTransaction(

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID,

    @Column(
        name = "event_id",
        nullable = false,
        unique = true,
        updatable = false
    )
    val eventId: UUID,

    @Column(name = "habit_id", nullable = false)
    val habitId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "successful_day", nullable = false)
    val successfulDay: LocalDate,

    @Column(
        name = "monthly_spend",
        nullable = false,
        precision = 14,
        scale = 2
    )
    val monthlySpend: BigDecimal,

    @Column(
        name = "saved_amount",
        nullable = false,
        precision = 14,
        scale = 2
    )
    val savedAmount: BigDecimal,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
