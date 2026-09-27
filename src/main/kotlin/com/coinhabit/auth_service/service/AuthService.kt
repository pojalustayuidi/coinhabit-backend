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
    private val jwtService: JwtService
) {
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw UserAlreadyExistsException("Email уже занят")
        }

        val user = User(
            email = request.email,
            nickname = request.nickname,
            passwordHash = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Не удалось захешировать пароль")        )

        val savedUser = userRepository.save(user)

        // Безопасно извлекаем ID, гарантируя компилятору, что он не null (без использования !!)
        val userId = savedUser.id ?: throw IllegalStateException("Не удалось получить ID пользователя после сохранения")

        // Теперь userId точно не null, и toString() вернет строгую String
        val token = jwtService.generateToken(savedUser.email, savedUser.role, userId.toString())

        return AuthResponse(
            token = token,
            user = UserDto(userId, savedUser.email, savedUser.nickname, savedUser.role)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException("Неверный email или пароль")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Неверный email или пароль")
        }

        val userId = user.id ?: throw IllegalStateException("У пользователя в БД отсутствует ID")
        val token = jwtService.generateToken(user.email, user.role, userId.toString())

        return AuthResponse(
            token = token,
            user = UserDto(userId, user.email, user.nickname, user.role)
        )
    }

    fun getCurrentUser(email: String): UserDto {
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException("Пользователь не найден")

        val userId = user.id ?: throw IllegalStateException("У пользователя в БД отсутствует ID")
        return UserDto(userId, user.email, user.nickname, user.role)
    }
}