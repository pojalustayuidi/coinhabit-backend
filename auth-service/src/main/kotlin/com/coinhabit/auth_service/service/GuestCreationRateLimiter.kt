package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.exception.GuestRateLimitException
import org.springframework.stereotype.Component
import java.time.Instant

/** Bounded, per-instance MVP abuse guard. Use the socket address, never arbitrary forwarded headers. */
@Component
class GuestCreationRateLimiter {
    private val attempts = mutableMapOf<String, MutableList<Instant>>()
    private val global = mutableListOf<Instant>()

    @Synchronized
    fun acquire(address: String) {
        val now = Instant.now()
        val cutoff = now.minusSeconds(3600)
        global.removeIf { !it.isAfter(cutoff) }
        val iterator = attempts.entries.iterator()
        while (iterator.hasNext()) {
            val times = iterator.next().value
            times.removeIf { !it.isAfter(cutoff) }
            if (times.isEmpty()) iterator.remove()
        }
        val times = attempts[address]
        if (global.size >= 1000 || (times?.size ?: 0) >= 10 ||
            (times == null && attempts.size >= 10000)) {
            throw GuestRateLimitException("Слишком много гостевых сессий. Повторите позже")
        }
        attempts.getOrPut(address) { mutableListOf() }.add(now)
        global.add(now)
    }
}
