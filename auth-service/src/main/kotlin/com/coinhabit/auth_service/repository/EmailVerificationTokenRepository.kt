
package com.coinhabit.auth_service.repository

import com.coinhabit.auth_service.entity.EmailVerificationToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EmailVerificationTokenRepository :
    JpaRepository<EmailVerificationToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHash(
        tokenHash: String
    ): EmailVerificationToken?

    fun findAllByUserId(
        userId: UUID
    ): List<EmailVerificationToken>
}
