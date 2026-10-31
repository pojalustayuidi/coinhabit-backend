
package com.coinhabit.savings_service.goal

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/savings/goals")
class SavingsGoalController(
    private val goalService: SavingsGoalService,
    private val progressService: SavingsGoalProgressService
) {

    // Получить семь готовых шаблонов целей.
    @GetMapping("/presets")
    fun getPresets(): List<GoalPreset> {
        return GoalPresetCatalog.presets
    }

    // Получить свои цели с прогрессом.
    @GetMapping
    fun getMyGoals(
        @AuthenticationPrincipal userId: UUID
    ): List<SavingsGoalProgress> {

        return progressService.getGoalsWithProgress(userId)
    }

    // Получить одну свою цель.
    @GetMapping("/{goalId}")
    fun getMyGoal(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable goalId: UUID
    ): SavingsGoalProgress {

        return progressService.getGoalWithProgress(
            userId = userId,
            goalId = goalId
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Цель не найдена"
        )
    }

    // Создать новую финансовую цель.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createGoal(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: CreateSavingsGoalRequest
    ): SavingsGoalResponse {

        val goal = goalService.createGoal(
            userId = userId,
            title = request.title,
            targetAmount = request.targetAmount,
            presetId = request.presetId
        )

        return goal.toResponse()
    }

    // Изменить название и стоимость своей цели.
    @PutMapping("/{goalId}")
    fun updateGoal(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable goalId: UUID,
        @RequestBody request: UpdateSavingsGoalRequest
    ): SavingsGoalResponse {

        val updatedGoal = goalService.updateGoal(
            userId = userId,
            goalId = goalId,
            title = request.title,
            targetAmount = request.targetAmount
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Цель не найдена"
        )

        return updatedGoal.toResponse()
    }

    // Удалить свою цель.
    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGoal(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable goalId: UUID
    ) {

        val deleted = goalService.deleteGoal(
            userId = userId,
            goalId = goalId
        )

        if (!deleted) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Цель не найдена"
            )
        }
    }

    // Превращаем ошибки валидации в HTTP 400.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidationError(
        exception: IllegalArgumentException
    ): ResponseEntity<Map<String, String>> {

        return ResponseEntity
            .badRequest()
            .body(
                mapOf(
                    "error" to (
                            exception.message
                                ?: "Некорректные данные цели"
                            )
                )
            )
    }

    // Преобразуем сущность в ответ для Android.
    private fun SavingsGoal.toResponse(): SavingsGoalResponse {
        return SavingsGoalResponse(
            id = id,
            title = title,
            targetAmount = targetAmount,
            presetId = presetId,
            createdAt = createdAt
        )
    }
}

data class CreateSavingsGoalRequest(
    val title: String,
    val targetAmount: BigDecimal,
    val presetId: String? = null
)

data class UpdateSavingsGoalRequest(
    val title: String,
    val targetAmount: BigDecimal
)

data class SavingsGoalResponse(
    val id: UUID,
    val title: String,
    val targetAmount: BigDecimal,
    val presetId: String?,
    val createdAt: Instant
)
