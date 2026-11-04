package com.coinhabit.savings_service.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class GuestJwtTest {
    private val secret = "0123456789abcdef0123456789abcdef"
    private val service = JwtService(secret)
    private fun token(id: String?, expiry: Date? = Date(System.currentTimeMillis() + 60_000), signingSecret: String = secret): String =
        Jwts.builder().subject(id ?: "guest").claim("userId", id).claim("role", "USER")
            .issuedAt(Date()).expiration(expiry).signWith(Keys.hmacShaKeyFor(signingSecret.toByteArray())).compact()

    @Test
    fun `UUID subject guest JWT works and identities stay independent`() {
        val first = UUID.randomUUID().toString()
        val second = UUID.randomUUID().toString()
        assertEquals(first, service.extractUserId(token(first)).toString())
        assertEquals(second, service.extractUserId(token(second)).toString())
    }

    @Test
    fun `missing expiry wrong signature expired missing or invalid identity is rejected`() {
        val id = UUID.randomUUID().toString()
        for (jwt in listOf(token(id, null), token(id, Date(0)), token(null), token("not-a-uuid"),
            token(id, signingSecret = "abcdefghijklmnopqrstuvwxyz012345"), "invalid")) {
            assertThrows(Exception::class.java) { service.extractUserId(jwt) }
        }
    }
}
