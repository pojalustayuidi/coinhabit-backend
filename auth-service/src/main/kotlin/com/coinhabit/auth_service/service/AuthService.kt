
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
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Locale

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailVerificationService: EmailVerificationService,
    private val verificationEmailSender: VerificationEmailSender,
    private val verificationEmailRateLimiter: VerificationEmailRateLimiter
) {

    @Transactional
    fun register(request: RegisterRequest): UserDto {
        val email = request.email
            .trim()
            .lowercase(Locale.ROOT)

        if (userRepository.existsByEmail(email)) {
            throw UserAlreadyExistsException("Email уже занят")
        }

        val hashedPassword = passwordEncoder.encode(
            request.password
        ) ?: throw IllegalStateException(
            "Не удалось захешировать пароль"
        )

        val user = User(
            email = email,
            nickname = request.nickname.trim(),
            passwordHash = hashedPassword,
            emailVerified = false
        )

        val savedUser = userRepository.save(user)

        val userId = savedUser.id
            ?: throw IllegalStateException(
                "Не удалось получить ID пользователя"
            )

        val verificationToken =
            emailVerificationService.createVerificationToken(userId)

        sendVerificationEmailAfterCommit(
            email = savedUser.email,
            token = verificationToken
        )

        return UserDto(
            id = userId,
            email = savedUser.email,
            nickname = savedUser.nickname,
            role = savedUser.role
        )
    }

    @Transactional
    fun resendVerificationEmail(email: String) {
        val normalizedEmail = email
            .trim()
            .lowercase(Locale.ROOT)

        val allowed = verificationEmailRateLimiter
            .tryAcquire(normalizedEmail)

        if (!allowed) {
            return
        }

        val user = userRepository.findByEmail(
            normalizedEmail
        ) ?: return

        if (user.emailVerified == true) {
            return
        }

        val userId = user.id
            ?: throw IllegalStateException(
                "У пользователя отсутствует ID"
            )

        val verificationToken =
            emailVerificationService.createVerificationToken(userId)

        sendVerificationEmailAfterCommit(
            email = user.email,
            token = verificationToken
        )
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val email = request.email
            .trim()
            .lowercase(Locale.ROOT)

        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException(
                "Неверный email или пароль"
            )

        if (!passwordEncoder.matches(
                request.password,
                user.passwordHash
            )) {
            throw InvalidCredentialsException(
                "Неверный email или пароль"
            )
        }

        if (user.emailVerified != true) {
            throw InvalidCredentialsException(
                "Подтвердите email для входа"
            )
        }

        val userId = user.id
            ?: throw IllegalStateException(
                "У пользователя в БД отсутствует ID"
            )

        val token = jwtService.generateToken(
            user.email,
            user.role,
            userId.toString()
        )

        return AuthResponse(
            token = token,
            user = UserDto(
                id = userId,
                email = user.email,
                nickname = user.nickname,
                role = user.role
            )
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(email: String): UserDto {
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException(
                "Пользователь не найден"
            )

        val userId = user.id
            ?: throw IllegalStateException(
                "У пользователя в БД отсутствует ID"
            )

        return UserDto(
            id = userId,
            email = user.email,
            nickname = user.nickname,
            role = user.role
        )
    }

    private fun sendVerificationEmailAfterCommit(
        email: String,
        token: String
    ) {
        check(
            TransactionSynchronizationManager.isActualTransactionActive() &&
                    TransactionSynchronizationManager.isSynchronizationActive()
        ) {
            "Отправку письма необходимо планировать внутри транзакции"
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {

                override fun afterCommit() {
                    verificationEmailSender.sendVerificationEmail(
                        email = email,
                        rawToken = token
                    )
                }
            }
        )
    }
}
