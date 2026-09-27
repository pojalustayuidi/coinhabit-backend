package com.coinhabit.auth_service.entity

import jakarta.persistence.*
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

    @Column(nullable = false, name = "password_hash")
    val passwordHash: String,

    @Column(nullable = false)
    val role: String = "USER"
)