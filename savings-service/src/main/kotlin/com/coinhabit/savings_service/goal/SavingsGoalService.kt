
package com.coinhabit.savings_service.goal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class SavingsGoalService(
    private val goalRepository: SavingsGoalRepository
) {

    @Transactional
    fun createGoal(
        userId: UUID,
        title: String,
        targetAmount: BigDecimal,
        presetId: String? = null
    ): SavingsGoal {

        val normalizedTitle = validateTitle(title)
        val normalizedAmount = validateAmount(targetAmount)

        val normalizedPresetId = presetId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (normalizedPresetId != null) {
            require(
                GoalPresetCatalog.presets.any {
                    it.id == normalizedPresetId
                }
            ) {
                "Неизвестный шаблон цели"
            }
        }

        val goal = SavingsGoal(
            id = UUID.randomUUID(),
            userId = userId,
            title = normalizedTitle,
            targetAmount = normalizedAmount,
            presetId = normalizedPresetId
        )

        return goalRepository.save(goal)
    }

    @Transactional(readOnly = true)
    fun getGoals(userId: UUID): List<SavingsGoal> {

        return goalRepository
            .findAllByUserIdOrderByCreatedAtDesc(userId)
    }

    @Transactional(readOnly = true)
    fun getGoal(
        userId: UUID,
        goalId: UUID
    ): SavingsGoal? {

        return goalRepository.findByIdAndUserId(
            goalId,
            userId
        )
    }

    // Редактирование существующей цели.
    @Transactional
    fun updateGoal(
        userId: UUID,
        goalId: UUID,
        title: String,
        targetAmount: BigDecimal
    ): SavingsGoal? {

        // Ищем цель только у текущего пользователя.
        val goal = goalRepository.findByIdAndUserId(
            goalId,
            userId
        ) ?: return null

        // Проверяем новые значения.
        val normalizedTitle = validateTitle(title)
        val normalizedAmount = validateAmount(targetAmount)

        // Меняем только название и сумму.
        // ID цели, пользователь и шаблон сохраняются.
        goal.title = normalizedTitle
        goal.targetAmount = normalizedAmount

        return goalRepository.save(goal)
    }

    @Transactional
    fun deleteGoal(
        userId: UUID,
        goalId: UUID
    ): Boolean {

        val deletedCount = goalRepository
            .deleteByIdAndUserId(
                goalId,
                userId
            )

        return deletedCount > 0
    }

    private fun validateTitle(title: String): String {

        val normalizedTitle = title.trim()

        require(normalizedTitle.isNotBlank()) {
            "Название цели не может быть пустым"
        }

        require(normalizedTitle.length <= 120) {
            "Название цели не может быть длиннее 120 символов"
        }

        return normalizedTitle
    }

    private fun validateAmount(
        targetAmount: BigDecimal
    ): BigDecimal {

        val normalizedAmount = targetAmount.setScale(
            2,
            RoundingMode.HALF_UP
        )

        require(normalizedAmount > BigDecimal.ZERO) {
            "Сумма цели должна быть больше нуля"
        }

        require(normalizedAmount.precision() <= 14) {
            "Сумма цели слишком большая"
        }

        return normalizedAmount
    }
}
