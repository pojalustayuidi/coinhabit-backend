
package com.coinhabit.savings_service.goal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "savings_goals",
    indexes = [
        Index(
            name = "idx_savings_goals_user_id",
            columnList = "user_id"
        )
    ]
)
class SavingsGoal(

    @Id
    @Column(
        nullable = false,
        updatable = false
    )
    val id: UUID,

    @Column(
        name = "user_id",
        nullable = false,
        updatable = false
    )
    val userId: UUID,

    @Column(
        nullable = false,
        length = 120
    )
    var title: String,

    @Column(
        name = "target_amount",
        nullable = false,
        precision = 14,
        scale = 2
    )
    var targetAmount: BigDecimal,

    @Column(
        name = "preset_id",
        length = 50
    )
    val presetId: String? = null,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    val createdAt: Instant = Instant.now()
)
