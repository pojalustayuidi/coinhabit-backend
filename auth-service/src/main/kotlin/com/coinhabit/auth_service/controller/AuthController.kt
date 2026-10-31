
package com.coinhabit.auth_service.controller

import com.coinhabit.auth_service.dto.AuthResponse
import com.coinhabit.auth_service.dto.LoginRequest
import com.coinhabit.auth_service.dto.RegisterRequest
import com.coinhabit.auth_service.dto.UserDto
import com.coinhabit.auth_service.service.AuthService
import com.coinhabit.auth_service.service.EmailVerificationService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "Bearer Authentication")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): RegistrationResponse {

        val user = authService.register(request)

        return RegistrationResponse(
            message = "Регистрация успешна. Проверьте почту для подтверждения email.",
            user = user
        )
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): AuthResponse {
        return authService.login(request)
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.OK)
    fun resendVerificationEmail(
        @Valid @RequestBody request: ResendVerificationRequest
    ): Map<String, String> {

        authService.resendVerificationEmail(request.email)

        return mapOf(
            "message" to
                    "Если адрес зарегистрирован и ещё не подтверждён, письмо будет отправлено"
        )
    }

    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam token: String
    ): ResponseEntity<Map<String, String>> {

        val verified =
            emailVerificationService.verifyEmail(token)

        return if (verified) {
            ResponseEntity.ok(
                mapOf(
                    "message" to
                            "Электронная почта успешно подтверждена"
                )
            )
        } else {
            ResponseEntity.badRequest().body(
                mapOf(
                    "error" to
                            "Ссылка недействительна или срок её действия истёк"
                )
            )
        }
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    fun getMe(): UserDto {
        val authentication =
            SecurityContextHolder.getContext().authentication

        val email = authentication?.name
            ?: throw RuntimeException(
                "Пользователь не авторизован"
            )

        return authService.getCurrentUser(email)
    }
}

data class RegistrationResponse(
    val message: String,
    val user: UserDto
)

data class ResendVerificationRequest(
    @field:NotBlank(message = "Email не может быть пустым")
    @field:Email(message = "Некорректный формат email")
    val email: String
)
