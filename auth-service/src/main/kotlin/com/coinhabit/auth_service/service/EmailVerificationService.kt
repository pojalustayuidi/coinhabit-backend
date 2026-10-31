
package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.entity.EmailVerificationToken
import com.coinhabit.auth_service.repository.EmailVerificationTokenRepository
import com.coinhabit.auth_service.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

@Service
class EmailVerificationService(
    private val userRepository: UserRepository,
    private val tokenRepository: EmailVerificationTokenRepository
) {

    private val secureRandom = SecureRandom()

    private val tokenLifetime = Duration.ofHours(24)

    private val tokenPattern = Regex("^[A-Za-z0-9_-]{43}$")

    @Transactional
    fun createVerificationToken(userId: UUID): String {
        val user = userRepository.findById(userId)
            .orElseThrow {
                NoSuchElementException("Пользователь не найден")
            }

        require(user.emailVerified != true) {
            "Email уже подтверждён"
        }

        val now = Instant.now()

        // Аннулируем ранее выданные токены.
        val previousTokens =
            tokenRepository.findAllByUserId(userId)

        previousTokens
            .filter { it.usedAt == null }
            .forEach { it.usedAt = now }

        tokenRepository.saveAll(previousTokens)

        // Генерируем 32 криптографически случайных байта.
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)

        // URL-safe токен без символов +, / и =.
        val rawToken = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes)

        val verificationToken = EmailVerificationToken(
            userId = userId,
            tokenHash = hashToken(rawToken),
            expiresAt = now.plus(tokenLifetime),
            createdAt = now
        )

        tokenRepository.save(verificationToken)

        // Открытый токен возвращаем только для отправки письма.
        // В PostgreSQL сохраняется исключительно его хеш.
        return rawToken
    }

    @Transactional
    fun verifyEmail(rawToken: String): Boolean {

        if (!tokenPattern.matches(rawToken)) {
            return false
        }

        val tokenHash = hashToken(rawToken)

        val token = tokenRepository.findByTokenHash(tokenHash)
            ?: return false

        val now = Instant.now()

        // Уже использованный токен.
        if (token.usedAt != null) {
            return false
        }

        // Просроченный токен.
        if (!now.isBefore(token.expiresAt)) {
            return false
        }

        val user = userRepository.findById(token.userId)
            .orElse(null)
            ?: return false

        user.emailVerified = true
        token.usedAt = now

        userRepository.save(user)
        tokenRepository.save(token)

        return true
    }

    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(
                rawToken.toByteArray(StandardCharsets.UTF_8)
            )

        return HexFormat.of().formatHex(digest)
    }
}
