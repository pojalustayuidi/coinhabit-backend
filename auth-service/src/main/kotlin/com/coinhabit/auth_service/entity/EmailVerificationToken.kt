
package com.coinhabit.auth_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "email_verification_tokens",
    indexes = [
        Index(
            name = "idx_email_verification_tokens_user_id",
            columnList = "user_id"
        )
    ]
)
class EmailVerificationToken(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(
        name = "user_id",
        nullable = false,
        updatable = false
    )
    val userId: UUID,

    @Column(
        name = "token_hash",
        nullable = false,
        unique = true,
        length = 64,
        updatable = false
    )
    val tokenHash: String,

    @Column(
        name = "expires_at",
        nullable = false,
        updatable = false
    )
    val expiresAt: Instant,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    val createdAt: Instant = Instant.now(),

    @Column(name = "used_at")
    var usedAt: Instant? = null
)
