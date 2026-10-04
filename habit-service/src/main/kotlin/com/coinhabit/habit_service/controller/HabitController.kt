package com.coinhabit.habit_service.controller

import com.coinhabit.habit_service.dto.HabitRequest
import com.coinhabit.habit_service.dto.HabitResponse
import com.coinhabit.habit_service.service.HabitService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/habits")
@Validated
@SecurityRequirement(name = "Bearer Authentication")
class HabitController(
    private val habitService: HabitService
) {

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val userIdString = authentication?.principal as? String
            ?: throw SecurityException("Пользователь не авторизован")
        return UUID.fromString(userIdString)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHabits(@RequestBody @Valid requests: List<@Valid HabitRequest>) {
        val userId = getCurrentUserId()
        habitService.createHabits(userId, requests)
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getUserHabits(): List<HabitResponse> {
        val userId = getCurrentUserId()
        return habitService.getUserHabits(userId)
    }

    @PostMapping("/{id}/relapse")
    @ResponseStatus(HttpStatus.OK)
    fun relapseHabit(@PathVariable id: UUID) {
        val userId = getCurrentUserId()
        habitService.relapseHabit(id, userId)
    }
}