
package com.coinhabit.auth_service.controller

import com.coinhabit.auth_service.dto.AuthResponse
import com.coinhabit.auth_service.dto.LoginRequest
import com.coinhabit.auth_service.dto.RegisterRequest
import com.coinhabit.auth_service.dto.UserDto
import com.coinhabit.auth_service.service.AuthService
import com.coinhabit.auth_service.service.EmailVerificationService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import java.util.UUID
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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
    ): AuthResponse = authService.register(request)

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    fun guest(request: HttpServletRequest): AuthResponse = authService.createGuest(request.remoteAddr)

    @PostMapping("/guest/upgrade")
    fun upgrade(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.upgradeGuest(currentUserId(), request)

    @PostMapping("/request-verification")
    fun requestVerification(): Map<String, String> {
        authService.requestVerification(currentUserId())
        return mapOf("message" to "Запрос принят. При отсутствии подтверждения будет предпринята отправка письма")
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
        return authService.getCurrentUser(currentUserId())
    }

    private fun currentUserId(): UUID = UUID.fromString(
        SecurityContextHolder.getContext().authentication.name
    )
}

data class ResendVerificationRequest(
    @field:NotBlank(message = "Email не может быть пустым")
    @field:Email(message = "Некорректный формат email")
    @field:Size(max = 254)
    val email: String
)
