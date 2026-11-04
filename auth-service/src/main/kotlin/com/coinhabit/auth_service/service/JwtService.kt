
package com.coinhabit.auth_service.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

data class JwtIdentity(
    val subject: String,
    val userId: UUID
)

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secretKey: String,

    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long,

    @Value("\${jwt.guest-expiration:604800000}")
    private val guestExpiration: Long = 604800000
) {

    private val signingKey: SecretKey
        get() = Keys.hmacShaKeyFor(
            secretKey.toByteArray(StandardCharsets.UTF_8)
        )

    fun generateToken(
        subject: String,
        role: String,
        userId: String,
        guest: Boolean = false
    ): String {
        val lifetime = if (guest) guestExpiration else jwtExpiration
        require(lifetime in 60_000L..604_800_000L) { "JWT lifetime must be between one minute and seven days" }
        val now = System.currentTimeMillis()

        return Jwts.builder()
            .subject(subject)
            .claim("role", role)
            .claim("userId", userId)
            .issuedAt(Date(now))
            .expiration(Date(now + lifetime))
            .signWith(signingKey)
            .compact()
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            val expiration = extractAllClaims(token).expiration
            expiration != null && expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    fun extractIdentity(token: String): JwtIdentity? {
        return try {
            val claims = extractAllClaims(token)

            val subject = claims.subject
                ?.takeIf { it.isNotBlank() }
                ?: return null

            val userIdValue = claims["userId"] as? String
                ?: return null

            val userId = UUID.fromString(userIdValue)

            val expiration = claims.expiration
                ?: return null

            if (!expiration.after(Date())) {
                return null
            }

            JwtIdentity(
                subject = subject,
                userId = userId
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
