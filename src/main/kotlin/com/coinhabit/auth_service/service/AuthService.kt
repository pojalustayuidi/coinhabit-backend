package com.coinhabit.auth_service.repository

import com.coinhabit.auth_service.dto.AuthResponse
import com.coinhabit.auth_service.dto.LoginRequest
import com.coinhabit.auth_service.dto.RegisterRequest
import com.coinhabit.auth_service.dto.UserDto
import com.coinhabit.auth_service.entity.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            // Временно бросаем базовое исключение, позже заменим на кастомное для единого формата ошибок
            throw RuntimeException("Email уже занят")
        }

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )

        val savedUser = userRepository.save(user)

        // Заглушка до реализации генерации JWT
        val token = "dummy-jwt-token"

        return AuthResponse(
            token = token,
            user = UserDto(savedUser.id!!, savedUser.email, savedUser.role)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw RuntimeException("Неверный email или пароль")

        // matches() безопасно сравнивает введенный текст с хешем из БД
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw RuntimeException("Неверный email или пароль")
        }

        val token = "dummy-jwt-token"

        return AuthResponse(
            token = token,
            user = UserDto(user.id!!, user.email, user.role)
        )
    }
}
