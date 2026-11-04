package com.coinhabit.auth_service.repository

import com.coinhabit.auth_service.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    fun findForUpdate(@Param("id") id: UUID): User?

    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}