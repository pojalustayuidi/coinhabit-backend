package com.coinhabit.auth_service.controller

import com.coinhabit.auth_service.dto.HabitRequest
import com.coinhabit.auth_service.service.HabitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/habits")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
class HabitController(
    private val habitService: HabitService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Сохранить список привычек пользователя")
    fun createHabits(@RequestBody @Valid requests: List<@Valid HabitRequest>) {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication?.name
            ?: throw RuntimeException("Пользователь не авторизован")

        habitService.saveHabits(email, requests)
    }
}