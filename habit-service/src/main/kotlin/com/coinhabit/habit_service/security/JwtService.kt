package com.coinhabit.habit_service.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private var secretKey: String
) {
    private val signingKey: SecretKey
        get() = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun extractUserId(token: String): String {
        val claims = extractAllClaims(token)
        require(claims.expiration?.after(Date()) == true) { "JWT expiry required" }
        return UUID.fromString(claims.get("userId", String::class.java)).toString()
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            extractAllClaims(token).expiration?.after(Date()) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
    }
}