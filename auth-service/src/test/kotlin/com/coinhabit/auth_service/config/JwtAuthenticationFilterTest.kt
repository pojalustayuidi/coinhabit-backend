
package com.coinhabit.auth_service.config

import com.coinhabit.auth_service.entity.User
import com.coinhabit.auth_service.repository.UserRepository
import com.coinhabit.auth_service.service.JwtIdentity
import com.coinhabit.auth_service.service.JwtService
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class JwtAuthenticationFilterTest {

    private val jwtService = Mockito.mock(JwtService::class.java)

    private val userRepository =
        Mockito.mock(UserRepository::class.java)

    private val filter = JwtAuthenticationFilter(
        jwtService = jwtService,
        userRepository = userRepository
    )

    private val email = "tester@example.com"
    private val userId = UUID.randomUUID()
    private val token = "test-jwt"

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        Mockito.reset(jwtService, userRepository)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `verified user with matching UUID is authenticated`() {
        val user = mockUser(
            id = userId,
            verified = true
        )

        prepareIdentity(user)

        val chainCalled = executeFilter(token)

        val authentication =
            SecurityContextHolder.getContext().authentication

        assertTrue(chainCalled)
        assertEquals(email, authentication?.principal)
        assertTrue(
            authentication?.authorities?.any {
                it.authority == "ROLE_USER"
            } == true
        )
    }

    @Test
    fun `JWT with different user UUID is rejected`() {
        val user = mockUser(
            id = UUID.randomUUID(),
            verified = true
        )

        prepareIdentity(user)

        val chainCalled = executeFilter(token)

        assertTrue(chainCalled)
        assertNull(
            SecurityContextHolder.getContext().authentication
        )
    }

    @Test
    fun `unverified user is not authenticated`() {
        val user = mockUser(
            id = userId,
            verified = false
        )

        prepareIdentity(user)

        val chainCalled = executeFilter(token)

        assertTrue(chainCalled)
        assertNull(
            SecurityContextHolder.getContext().authentication
        )
    }

    @Test
    fun `missing user is not authenticated`() {
        Mockito.`when`(
            jwtService.extractIdentity(token)
        ).thenReturn(
            JwtIdentity(email, userId)
        )

        Mockito.`when`(
            userRepository.findByEmail(email)
        ).thenReturn(null)

        val chainCalled = executeFilter(token)

        assertTrue(chainCalled)
        assertNull(
            SecurityContextHolder.getContext().authentication
        )
    }

    @Test
    fun `invalid JWT is rejected`() {
        Mockito.`when`(
            jwtService.extractIdentity(token)
        ).thenReturn(null)

        val chainCalled = executeFilter(token)

        assertTrue(chainCalled)
        assertNull(
            SecurityContextHolder.getContext().authentication
        )

        Mockito.verifyNoInteractions(userRepository)
    }

    @Test
    fun `request without JWT is not authenticated`() {
        val chainCalled = executeFilter(null)

        assertTrue(chainCalled)
        assertNull(
            SecurityContextHolder.getContext().authentication
        )

        Mockito.verifyNoInteractions(
            jwtService,
            userRepository
        )
    }

    private fun mockUser(
        id: UUID,
        verified: Boolean
    ): User {
        val user = Mockito.mock(User::class.java)

        Mockito.`when`(user.id).thenReturn(id)
        Mockito.`when`(user.email).thenReturn(email)
        Mockito.`when`(user.emailVerified).thenReturn(verified)
        Mockito.`when`(user.role).thenReturn("USER")

        return user
    }

    private fun prepareIdentity(user: User) {
        Mockito.`when`(
            jwtService.extractIdentity(token)
        ).thenReturn(
            JwtIdentity(email, userId)
        )

        Mockito.`when`(
            userRepository.findByEmail(email)
        ).thenReturn(user)
    }

    private fun executeFilter(jwt: String?): Boolean {
        val request = MockHttpServletRequest(
            "GET",
            "/api/auth/me"
        )

        val response = MockHttpServletResponse()

        if (jwt != null) {
            request.addHeader(
                "Authorization",
                "Bearer $jwt"
            )
        }

        var chainCalled = false

        val chain = FilterChain { _, _ ->
            chainCalled = true
        }

        filter.doFilter(request, response, chain)

        return chainCalled
    }
}
