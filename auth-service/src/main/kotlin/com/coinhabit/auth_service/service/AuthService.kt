package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.dto.AuthResponse
import com.coinhabit.auth_service.dto.LoginRequest
import com.coinhabit.auth_service.dto.RegisterRequest
import com.coinhabit.auth_service.dto.UserDto
import com.coinhabit.auth_service.entity.User
import com.coinhabit.auth_service.exception.InvalidCredentialsException
import com.coinhabit.auth_service.exception.UserAlreadyExistsException
import com.coinhabit.auth_service.exception.AccountStateException
import com.coinhabit.auth_service.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Locale
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailVerificationService: EmailVerificationService,
    private val verificationEmailSender: VerificationEmailSender,
    private val verificationEmailRateLimiter: VerificationEmailRateLimiter,
    private val guestCreationRateLimiter: GuestCreationRateLimiter
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        checkEmailAvailable(email)
        val user = userRepository.saveAndFlush(
            User(
                email = email,
                nickname = request.nickname.trim(),
                passwordHash = hashPassword(request.password)
            )
        )
        return authorize(user)
    }

    @Transactional
    fun createGuest(clientAddress: String): AuthResponse {
        guestCreationRateLimiter.acquire(clientAddress)
        val guest = userRepository.saveAndFlush(User(nickname = "Гость", guest = true))
        return authorize(guest)
    }

    @Transactional
    fun upgradeGuest(userId: UUID, request: RegisterRequest): AuthResponse {
        // Блокировка не даёт двум запросам одновременно зарегистрировать одного гостя.
        val user = userRepository.findForUpdate(userId)
            ?: throw InvalidCredentialsException("Пользователь не найден")
        if (!user.guest) throw AccountStateException("Аккаунт уже зарегистрирован")
        val email = normalizeEmail(request.email)
        checkEmailAvailable(email)
        user.email = email
        user.passwordHash = hashPassword(request.password)
        user.nickname = request.nickname.trim()
        user.emailVerified = false
        user.guest = false
        userRepository.saveAndFlush(user)
        return authorize(user)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(normalizeEmail(request.email))
            ?: throw InvalidCredentialsException("Неверный email или пароль")
        val passwordHash = user.passwordHash
        if (user.guest || passwordHash == null ||
            !passwordEncoder.matches(request.password, passwordHash)) {
            throw InvalidCredentialsException("Неверный email или пароль")
        }
        return authorize(user)
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(userId: UUID): UserDto = toDto(
        userRepository.findById(userId).orElseThrow {
            InvalidCredentialsException("Пользователь не найден")
        }
    )

    @Transactional
    fun requestVerification(userId: UUID) {
        val user = userRepository.findById(userId).orElseThrow {
            InvalidCredentialsException("Пользователь не найден")
        }
        if (user.guest) throw AccountStateException("Сначала зарегистрируйте гостевой аккаунт")
        resendVerificationEmail(requireNotNull(user.email))
    }

    @Transactional
    fun resendVerificationEmail(email: String) {
        val normalized = normalizeEmail(email)
        if (!verificationEmailRateLimiter.tryAcquire(normalized)) return
        val user = userRepository.findByEmail(normalized) ?: return
        if (user.guest || user.emailVerified == true) return
        val token = emailVerificationService.createVerificationToken(requireNotNull(user.id))
        check(TransactionSynchronizationManager.isSynchronizationActive())
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                try {
                    verificationEmailSender.sendVerificationEmail(normalized, token)
                } catch (_: Exception) {
                    logger.warn("Не удалось отправить письмо подтверждения")
                }
            }
        })
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase(Locale.ROOT)

    private fun checkEmailAvailable(email: String) {
        if (userRepository.existsByEmail(email)) throw UserAlreadyExistsException("Email уже занят")
    }

    private fun hashPassword(password: String): String {
        // Ограничение BCrypt считается в байтах UTF-8, а не в символах.
        require(password.toByteArray(Charsets.UTF_8).size <= 72) { "Пароль длиннее 72 байт UTF-8" }
        return requireNotNull(passwordEncoder.encode(password))
    }

    private fun authorize(user: User): AuthResponse {
        val userId = requireNotNull(user.id).toString()
        val token = jwtService.generateToken(
            subject = userId,
            role = user.role,
            userId = userId,
            guest = user.guest
        )
        return AuthResponse(token, toDto(user))
    }

    private fun toDto(user: User) = UserDto(
        id = requireNotNull(user.id),
        email = user.email.orEmpty(),
        nickname = user.nickname,
        role = user.role,
        emailVerified = user.emailVerified == true,
        isGuest = user.guest
    )
}
