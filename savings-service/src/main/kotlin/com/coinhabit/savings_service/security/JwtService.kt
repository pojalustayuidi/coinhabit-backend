
package com.coinhabit.savings_service.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secret: String
) {

    private val signingKey: SecretKey = Keys.hmacShaKeyFor(
        secret.toByteArray(StandardCharsets.UTF_8)
    )

    fun extractUserId(token: String): UUID {


        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload


        val userId = claims.get(
            "userId",
            String::class.java
        ) ?: throw IllegalArgumentException(
            "JWT не содержит идентификатор пользователя"
        )

        return UUID.fromString(userId)
    }
}
