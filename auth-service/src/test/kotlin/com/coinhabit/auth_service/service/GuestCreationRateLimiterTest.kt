package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.exception.GuestRateLimitException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GuestCreationRateLimiterTest {
    @Test
    fun `one client cannot create unlimited guests while another can proceed`() {
        val limiter = GuestCreationRateLimiter()
        repeat(10) { limiter.acquire("192.0.2.1") }
        assertThrows(GuestRateLimitException::class.java) { limiter.acquire("192.0.2.1") }
        limiter.acquire("192.0.2.2")
    }

    @Test
    fun `rotating client addresses cannot bypass the global budget`() {
        val limiter = GuestCreationRateLimiter()
        repeat(1000) { limiter.acquire("client-$it") }
        assertThrows(GuestRateLimitException::class.java) { limiter.acquire("another") }
    }
}
