package com.coinhabit.auth_service.config

import com.coinhabit.auth_service.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        // 2. Если заголовка нет или он не начинается с "Bearer ", пропускаем запрос дальше
        // (его заблокирует Spring Security, если эндпоинт закрыт)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        // 3. Достаем сам токен (отрезаем первые 7 символов: "Bearer ")
        val jwt = authHeader.substring(7)

        val userEmail = try {
            jwtService.extractEmail(jwt)
        } catch (e: Exception) {
            null
        }

        // 5. Если email есть, а пользователь еще не авторизован в текущем контексте Spring
        if (userEmail != null && SecurityContextHolder.getContext().authentication == null) {

            // 6. Проверяем валидность токена
            if (jwtService.isTokenValid(jwt)) {
                // Создаем объект аутентификации. В микросервисах нам не нужно идти в БД,
                // мы доверяем данным из токена (Stateless подход).
                val authToken = UsernamePasswordAuthenticationToken(
                    userEmail,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )

                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}