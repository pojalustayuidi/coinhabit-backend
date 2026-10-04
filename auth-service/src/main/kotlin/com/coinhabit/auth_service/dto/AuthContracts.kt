    package com.coinhabit.auth_service.dto

    import jakarta.validation.constraints.Email
    import jakarta.validation.constraints.NotBlank
    import jakarta.validation.constraints.Size
    import java.util.UUID

    data class RegisterRequest(
        @field:NotBlank(message = "Email не может быть пустым")
        @field:Email(message = "Некорректный формат email")
        val email: String,

        @field:NotBlank(message = "Никнейм не может быть пустым")
        val nickname: String,

        @field:NotBlank(message = "Пароль не может быть пустым")
        @field:Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        val password: String
    )

    data class LoginRequest(
        @field:NotBlank(message = "Email не может быть пустым")
        @field:Email(message = "Некорректный формат email")
        val email: String,

        @field:NotBlank(message = "Пароль не может быть пустым")
        val password: String
    )

    data class AuthResponse(
        val token: String,
        val user: UserDto
    )

    data class UserDto(
        val id: UUID,
        val email: String,
        val nickname: String,
        val role: String
    )