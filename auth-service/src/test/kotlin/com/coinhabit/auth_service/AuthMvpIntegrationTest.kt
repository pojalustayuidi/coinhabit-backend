package com.coinhabit.auth_service

import com.coinhabit.auth_service.entity.User
import com.coinhabit.auth_service.repository.UserRepository
import com.coinhabit.auth_service.service.VerificationEmailSender
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mail.MailSendException
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.JsonNode
import java.util.UUID

/** Real PostgreSQL, migrations, MVC validation, security filters and transaction commits. */
@SpringBootTest
class AuthMvpIntegrationTest {
    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var users: UserRepository
    @MockitoBean lateinit var sender: VerificationEmailSender
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity()).build()
        reset(sender)
    }

    private fun registration(email: String = "${UUID.randomUUID()}@example.com") =
        """{"email":"$email","nickname":"Tester","password":"password123"}"""

    private fun postJson(path: String, body: String, token: String? = null, statusCode: Int = 200): JsonNode {
        val request = post("/api/auth/$path").contentType(MediaType.APPLICATION_JSON).content(body)
        if (token != null) request.header("Authorization", "Bearer $token")
        val response = mvc.perform(request).andExpect(status().`is`(statusCode)).andReturn().response
        return if (response.contentAsString.isBlank()) mapper.createObjectNode() else mapper.readTree(response.contentAsString)
    }

    @Test
    fun `registration login and me work without email confirmation or SMTP`() {
        doThrow(MailSendException("SMTP offline")).`when`(sender).sendVerificationEmail(anyString(), anyString())
        val email = "${UUID.randomUUID()}@example.com"
        val registered = postJson("register", registration(email), statusCode = 201)
        assertFalse(registered["user"]["emailVerified"].asBoolean())
        val token = registered["token"].asText()
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk).andExpect(jsonPath("$.id").value(registered["user"]["id"].asText()))
        postJson("login", """{"email":"$email","password":"password123"}""")
        verifyNoInteractions(sender)
        postJson("request-verification", "{}", token)
        verify(sender).sendVerificationEmail(eq(email), anyString())
        // A failed send must not undo account creation or prevent another login.
        postJson("login", """{"email":"$email","password":"password123"}""")
    }

    @Test
    fun `confirmation is opt in one use and throttled`() {
        val email = "${UUID.randomUUID()}@example.com"
        val registered = postJson("register", registration(email), statusCode = 201)
        val token = registered["token"].asText()
        verifyNoInteractions(sender)
        postJson("request-verification", "{}", token)
        val captured = ArgumentCaptor.forClass(String::class.java)
        verify(sender).sendVerificationEmail(eq(email), captured.capture())
        postJson("request-verification", "{}", token)
        verify(sender, times(1)).sendVerificationEmail(eq(email), anyString())
        mvc.perform(get("/api/auth/verify-email").param("token", captured.value)).andExpect(status().isOk)
        mvc.perform(get("/api/auth/verify-email").param("token", captured.value)).andExpect(status().isBadRequest)
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer $token"))
            .andExpect(jsonPath("$.emailVerified").value(true))
        assertTrue(users.findByEmail(email)?.emailVerified == true)
    }

    @Test
    fun `guests have independent identities and upgrade preserves UUID`() {
        val first = postJson("guest", "{}", statusCode = 201)
        val second = postJson("guest", "{}", statusCode = 201)
        assertNotEquals(first["user"]["id"].asText(), second["user"]["id"].asText())
        assertTrue(first["user"]["isGuest"].asBoolean())
        assertEquals("", first["user"]["email"].asText())
        val upgraded = postJson("guest/upgrade", registration(), first["token"].asText())
        assertEquals(first["user"]["id"].asText(), upgraded["user"]["id"].asText())
        assertFalse(upgraded["user"]["isGuest"].asBoolean())
        assertTrue(users.findById(UUID.fromString(second["user"]["id"].asText())).get().guest)
        postJson("guest/upgrade", registration(), first["token"].asText(), 409)
        postJson("request-verification", "{}", second["token"].asText(), 409)
    }

    @Test
    fun `occupied email and invalid input do not change guest`() {
        val email = "${UUID.randomUUID()}@example.com"
        postJson("register", registration(email), statusCode = 201)
        postJson("register", registration(email.uppercase()), statusCode = 409)
        val guest = postJson("guest", "{}", statusCode = 201)
        postJson("guest/upgrade", registration(email), guest["token"].asText(), 409)
        assertTrue(users.findById(UUID.fromString(guest["user"]["id"].asText())).get().guest)
        postJson("register", """{"email":"broken","nickname":"","password":"1"}""", statusCode = 400)
        postJson("login", """{"email":"$email","password":"wrong"}""", statusCode = 401)
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized)
        postJson("guest/upgrade", registration(), "invalid", 401)
    }

    @Test
    fun `legacy null verification is false and legacy verified account remains verified`() {
        for (verified in listOf(null, true)) {
            val email = "${UUID.randomUUID()}@example.com"
            // Existing accounts keep their flags; migration also maps historic null to false.
            val user = users.saveAndFlush(User(email = email, nickname = "Legacy",
                passwordHash = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"),
                emailVerified = verified))
            val response = postJson("login", """{"email":"$email","password":"password123"}""")
            assertEquals(user.id.toString(), response["user"]["id"].asText())
            assertEquals(verified == true, response["user"]["emailVerified"].asBoolean())
        }
    }
}
