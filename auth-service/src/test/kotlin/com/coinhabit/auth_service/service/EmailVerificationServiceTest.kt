
package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.entity.EmailVerificationToken
import com.coinhabit.auth_service.entity.User
import com.coinhabit.auth_service.repository.EmailVerificationTokenRepository
import com.coinhabit.auth_service.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat
import java.util.Optional
import java.util.UUID

class EmailVerificationServiceTest {

    private val userRepository =
        Mockito.mock(UserRepository::class.java)

    private val tokenRepository =
        Mockito.mock(EmailVerificationTokenRepository::class.java)

    private val service = EmailVerificationService(
        userRepository = userRepository,
        tokenRepository = tokenRepository
    )

    private val userId = UUID.randomUUID()
    private val rawToken = "A".repeat(43)

    @BeforeEach
    fun setUp() {
        Mockito.reset(userRepository, tokenRepository)
    }

    @Test
    fun `invalid token format is rejected`() {
        val result = service.verifyEmail("invalid-token")

        assertFalse(result)

        Mockito.verifyNoInteractions(
            userRepository,
            tokenRepository
        )
    }

    @Test
    fun `unknown token is rejected`() {
        val result = service.verifyEmail(rawToken)

        assertFalse(result)
        Mockito.verifyNoInteractions(userRepository)
    }

    @Test
    fun `expired token is rejected`() {
        val token = mockToken(
            expiresAt = Instant.now().minusSeconds(3600)
        )

        prepareToken(token)

        val result = service.verifyEmail(rawToken)

        assertFalse(result)
        Mockito.verifyNoInteractions(userRepository)
    }

    @Test
    fun `already used token is rejected`() {
        val token = mockToken(
            expiresAt = Instant.now().plusSeconds(3600),
            usedAt = Instant.now().minusSeconds(60)
        )

        prepareToken(token)

        val result = service.verifyEmail(rawToken)

        assertFalse(result)
        Mockito.verifyNoInteractions(userRepository)
    }

    @Test
    fun `valid token confirms user email`() {
        val token = mockToken(
            expiresAt = Instant.now().plusSeconds(3600)
        )

        val user = Mockito.mock(User::class.java)

        prepareToken(token)

        Mockito.`when`(
            userRepository.findById(userId)
        ).thenReturn(Optional.of(user))

        val result = service.verifyEmail(rawToken)

        assertTrue(result)

        Mockito.verify(user).emailVerified = true
        Mockito.verify(userRepository).save(user)
        Mockito.verify(tokenRepository).save(token)
    }

    private fun mockToken(
        expiresAt: Instant,
        usedAt: Instant? = null
    ): EmailVerificationToken {
        val token = Mockito.mock(
            EmailVerificationToken::class.java
        )

        Mockito.`when`(token.userId).thenReturn(userId)
        Mockito.`when`(token.expiresAt).thenReturn(expiresAt)

        if (usedAt != null) {
            Mockito.`when`(token.usedAt).thenReturn(usedAt)
        }

        return token
    }

    private fun prepareToken(token: EmailVerificationToken) {
        Mockito.`when`(
            tokenRepository.findByTokenHash(hashToken(rawToken))
        ).thenReturn(token)
    }

    private fun hashToken(value: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))

        return HexFormat.of().formatHex(bytes)
    }
}
