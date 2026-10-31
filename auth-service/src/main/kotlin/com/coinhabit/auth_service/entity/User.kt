
package com.coinhabit.auth_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val nickname: String,

    @Column(
        nullable = false,
        name = "password_hash"
    )
    val passwordHash: String,

    @Column(nullable = false)
    val role: String = "USER",

    @Column(name = "email_verified")
    var emailVerified: Boolean? = false
)
