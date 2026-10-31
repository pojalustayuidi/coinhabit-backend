
package com.coinhabit.auth_service.service

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import java.util.Locale

@Component
class VerificationEmailRateLimiter {

    private val cooldown = Duration.ofMinutes(2)
    private val window = Duration.ofHours(24)

    private val maxEmailsPerWindow = 5
    private val maxTrackedAddresses = 10_000

    private val attemptsByEmail =
        mutableMapOf<String, MutableList<Instant>>()

    @Synchronized
    fun tryAcquire(email: String): Boolean {
        val now = Instant.now()
        val cutoff = now.minus(window)

        // Удаляем записи старше 24 часов,
        // чтобы не хранить их бесконечно.
        val iterator = attemptsByEmail.entries.iterator()

        while (iterator.hasNext()) {
            val attempts = iterator.next().value

            attempts.removeIf {
                !it.isAfter(cutoff)
            }

            if (attempts.isEmpty()) {
                iterator.remove()
            }
        }

        val normalizedEmail = email
            .trim()
            .lowercase(Locale.ROOT)

        val emailHash = hashEmail(normalizedEmail)

        val attempts = attemptsByEmail[emailHash]

        if (attempts == null) {
            // Ограничиваем размер памяти.
            if (attemptsByEmail.size >= maxTrackedAddresses) {
                return false
            }

            attemptsByEmail[emailHash] =
                mutableListOf(now)

            return true
        }

        // Максимум 5 запросов за 24 часа.
        if (attempts.size >= maxEmailsPerWindow) {
            return false
        }

        // Между запросами должно пройти 2 минуты.
        val lastAttempt = attempts.last()

        if (Duration.between(lastAttempt, now) < cooldown) {
            return false
        }

        attempts.add(now)

        return true
    }

    private fun hashEmail(email: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(
                email.toByteArray(StandardCharsets.UTF_8)
            )

        return HexFormat.of().formatHex(digest)
    }
}
