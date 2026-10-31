
package com.coinhabit.savings_service.security

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val authHeader = request.getHeader(
            HttpHeaders.AUTHORIZATION
        )

        // Если токен не передан, продолжаем обработку.
        // Позже SecurityConfig запретит доступ
        // к защищённым API без авторизации.
        if (
            authHeader == null ||
            !authHeader.startsWith("Bearer ")
        ) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader
            .removePrefix("Bearer ")
            .trim()

        // Проверяем подпись и срок действия JWT.
        val userId = try {
            jwtService.extractUserId(token)
        } catch (e: JwtException) {
            rejectRequest(response)
            return
        } catch (e: IllegalArgumentException) {
            rejectRequest(response)
            return
        }

        // Сохраняем подтверждённый userId
        // в контексте безопасности.
        val authentication =
            UsernamePasswordAuthenticationToken(
                userId,
                null,
                emptyList()
            )

        val context = SecurityContextHolder
            .createEmptyContext()

        context.authentication = authentication

        SecurityContextHolder.setContext(context)

        filterChain.doFilter(request, response)
    }

    private fun rejectRequest(
        response: HttpServletResponse
    ) {
        SecurityContextHolder.clearContext()

        response.sendError(
            HttpServletResponse.SC_UNAUTHORIZED,
            "Invalid or expired token"
        )
    }
}
