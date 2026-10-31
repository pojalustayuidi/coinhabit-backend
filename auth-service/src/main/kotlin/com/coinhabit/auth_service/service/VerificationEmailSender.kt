
package com.coinhabit.auth_service.service

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class VerificationEmailSender(
    private val mailSenderProvider: ObjectProvider<JavaMailSender>,

    @Value("\${coinhabit.mail.from:}")
    private val fromAddress: String,

    @Value("\${coinhabit.mail.verification-url:}")
    private val verificationUrl: String
) {

    fun sendVerificationEmail(
        email: String,
        rawToken: String
    ) {
        check(fromAddress.isNotBlank()) {
            "Не настроен адрес отправителя coinhabit.mail.from"
        }

        check(verificationUrl.isNotBlank()) {
            "Не настроена ссылка подтверждения email"
        }

        val mailSender = mailSenderProvider.getIfAvailable()
            ?: throw IllegalStateException(
                "SMTP не настроен. Укажите параметры почтового сервера."
            )

        val encodedToken = URLEncoder.encode(
            rawToken,
            StandardCharsets.UTF_8
        )

        val separator = if (verificationUrl.contains("?")) {
            "&"
        } else {
            "?"
        }

        val confirmationLink =
            "$verificationUrl${separator}token=$encodedToken"

        val message = SimpleMailMessage().apply {
            setFrom(fromAddress)
            setTo(email)
            subject = "CoinHabit — подтверждение электронной почты"

            text = """
                Привет!

                Спасибо за регистрацию в CoinHabit.

                Чтобы подтвердить свою электронную почту,
                перейди по ссылке:

                $confirmationLink

                Ссылка действует 24 часа и может быть
                использована только один раз.

                Если ты не регистрировался в CoinHabit,
                просто проигнорируй это письмо.

                Команда CoinHabit
            """.trimIndent()
        }

        mailSender.send(message)
    }
}
