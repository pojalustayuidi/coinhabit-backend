
package com.coinhabit.auth_service.exception

import org.junit.jupiter.api.Test
import org.springframework.mail.MailSendException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {

    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(TestMailController())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    @Test
    fun `SMTP failure returns HTTP 503`() {
        mockMvc.perform(
            get("/test/mail-failure")
        )
            .andExpect(
                status().isServiceUnavailable
            )
            .andExpect(
                jsonPath("$.status").value(503)
            )
            .andExpect(
                jsonPath("$.message").value(
                    "Почтовый сервис временно недоступен. " +
                            "Пожалуйста, повторите попытку позже."
                )
            )
    }
}

@RestController
class TestMailController {

    @GetMapping("/test/mail-failure")
    fun simulateMailFailure(): String {
        throw MailSendException(
            "Тестовая ошибка SMTP"
        )
    }
}
