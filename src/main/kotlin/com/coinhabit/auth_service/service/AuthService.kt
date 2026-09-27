package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.dto.AuthResponse
import com.coinhabit.auth_service.dto.LoginRequest
import com.coinhabit.auth_service.dto.RegisterRequest
import com.coinhabit.auth_service.dto.UserDto
import com.coinhabit.auth_service.entity.User
import com.coinhabit.auth_service.exception.InvalidCredentialsException
import com.coinhabit.auth_service.exception.UserAlreadyExistsException
import com.coinhabit.auth_service.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService // <-- Внедрили генератор токенов
) {
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw UserAlreadyExistsException("Email уже занят")
        }
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )

        val savedUser = userRepository.save(user)

        // Генерируем реальный JWT токен
        val token = jwtService.generateToken(
            email = savedUser.email,
            role = savedUser.role,
            userId = savedUser.id.toString()
        )

        return AuthResponse(
            token = token,
            user = UserDto(savedUser.id!!, savedUser.email, savedUser.role)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException("Неверный email или пароль")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Неверный email или пароль")
        }

        // Генерируем реальный JWT токен
        val token = jwtService.generateToken(
            email = user.email,
            role = user.role,
            userId = user.id.toString()
        )

        return AuthResponse(
            token = token,
            user = UserDto(user.id!!, user.email, user.role)
        )
    }
}