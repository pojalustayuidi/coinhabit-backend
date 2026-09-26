package com.coinhabit.auth_service.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expiration}") private val jwtExpiration: Long
) {

    private val signingKey: SecretKey
        get() = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun generateToken(email: String, role: String, userId: String): String {
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .claim("userId", userId)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(signingKey)
            .compact()
    }

    fun extractEmail(token: String): String {
        return extractAllClaims(token).subject
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            !isTokenExpired(token)
        } catch (e: Exception) {
            false
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractAllClaims(token).expiration.before(Date())
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}