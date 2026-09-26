package com.coinhabit.auth_service.controller

import com.coinhabit.auth_service.dto.AuthResponse
import com.coinhabit.auth_service.dto.LoginRequest
import com.coinhabit.auth_service.dto.RegisterRequest
import com.coinhabit.auth_service.repository.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request)
    }
}