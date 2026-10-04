package com.coinhabit.habit_service.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private var secretKey: String
) {
    private val signingKey: SecretKey
        get() = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun extractUserId(token: String): String {
        return extractAllClaims(token).get("userId", String::class.java)
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            !extractAllClaims(token).expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
    }
}