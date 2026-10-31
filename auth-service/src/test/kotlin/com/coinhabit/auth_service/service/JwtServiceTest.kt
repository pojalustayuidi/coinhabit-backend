
package com.coinhabit.auth_service.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

class JwtServiceTest {

    private val testSecret =
        "0123456789abcdef0123456789abcdef"

    private val jwtService = JwtService(
        secretKey = testSecret,
        jwtExpiration = 3_600_000L
    )

    @Test
    fun `valid token contains correct user identity`() {
        val userId = UUID.randomUUID()
        val email = "tester@example.com"

        val token = jwtService.generateToken(
            email = email,
            role = "USER",
            userId = userId.toString()
        )

        val identity = jwtService.extractIdentity(token)

        assertNotNull(identity)
        assertEquals(email, identity?.email)
        assertEquals(userId, identity?.userId)
    }

    @Test
    fun `token signed with another secret is rejected`() {
        val anotherJwtService = JwtService(
            secretKey = "abcdefghijklmnopqrstuvwxyz012345",
            jwtExpiration = 3_600_000L
        )

        val token = anotherJwtService.generateToken(
            email = "tester@example.com",
            role = "USER",
            userId = UUID.randomUUID().toString()
        )

        assertNull(jwtService.extractIdentity(token))
        assertFalse(jwtService.isTokenValid(token))
    }

    @Test
    fun `expired token is rejected`() {
        val expiredJwtService = JwtService(
            secretKey = testSecret,
            jwtExpiration = -60_000L
        )

        val token = expiredJwtService.generateToken(
            email = "tester@example.com",
            role = "USER",
            userId = UUID.randomUUID().toString()
        )

        assertNull(jwtService.extractIdentity(token))
        assertFalse(jwtService.isTokenValid(token))
    }

    @Test
    fun `token without userId is rejected`() {
        val now = System.currentTimeMillis()

        val key = Keys.hmacShaKeyFor(
            testSecret.toByteArray(StandardCharsets.UTF_8)
        )

        val token = Jwts.builder()
            .subject("tester@example.com")
            .issuedAt(Date(now))
            .expiration(Date(now + 3_600_000L))
            .signWith(key)
            .compact()

        assertNull(jwtService.extractIdentity(token))
    }

    @Test
    fun `malformed token is rejected`() {
        val token = "invalid.fake.token"

        assertNull(jwtService.extractIdentity(token))
        assertFalse(jwtService.isTokenValid(token))
    }
}
