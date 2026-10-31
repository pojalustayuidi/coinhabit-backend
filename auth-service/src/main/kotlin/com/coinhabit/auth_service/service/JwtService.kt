
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
    val email: String,
    val userId: UUID
)

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secretKey: String,

    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long
) {

    private val signingKey: SecretKey
        get() = Keys.hmacShaKeyFor(
            secretKey.toByteArray(StandardCharsets.UTF_8)
        )

    fun generateToken(
        email: String,
        role: String,
        userId: String
    ): String {
        val now = System.currentTimeMillis()

        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .claim("userId", userId)
            .issuedAt(Date(now))
            .expiration(Date(now + jwtExpiration))
            .signWith(signingKey)
            .compact()
    }

    fun extractEmail(token: String): String {
        return extractAllClaims(token).subject
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

            val email = claims.subject
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
                email = email,
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
