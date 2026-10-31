
package com.coinhabit.auth_service.config

import com.coinhabit.auth_service.repository.UserRepository
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
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (
            authHeader == null ||
            !authHeader.startsWith("Bearer ")
        ) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7).trim()

        if (
            token.isNotEmpty() &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val identity = jwtService.extractIdentity(token)

            if (identity != null) {
                val user = userRepository.findByEmail(
                    identity.email
                )

                if (
                    user != null &&
                    user.id == identity.userId &&
                    user.emailVerified == true
                ) {
                    val role = if (
                        user.role.startsWith("ROLE_")
                    ) {
                        user.role
                    } else {
                        "ROLE_${user.role}"
                    }

                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            user.email,
                            null,
                            listOf(
                                SimpleGrantedAuthority(role)
                            )
                        )

                    SecurityContextHolder.getContext()
                        .authentication = authentication
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
