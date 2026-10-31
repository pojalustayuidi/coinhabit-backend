
package com.coinhabit.savings_service.goal

import com.coinhabit.savings_service.service.SavingsSummaryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class SavingsGoalProgress(
    val goalId: UUID,
    val title: String,
    val presetId: String?,
    val targetAmount: BigDecimal,
    val totalSaved: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercent: BigDecimal,
    val achieved: Boolean
)

@Service
class SavingsGoalProgressService(
    private val goalService: SavingsGoalService,
    private val savingsSummaryService: SavingsSummaryService
) {

    @Transactional(readOnly = true)
    fun getGoalsWithProgress(
        userId: UUID
    ): List<SavingsGoalProgress> {

        val goals = goalService.getGoals(userId)

        if (goals.isEmpty()) {
            return emptyList()
        }

        // Общая подтверждённая экономия пользователя.
        // Получаем один раз для всех его целей.
        val totalSaved = savingsSummaryService
            .getConfirmedTotal(userId)

        return goals.map { goal ->
            calculateProgress(goal, totalSaved)
        }
    }

    @Transactional(readOnly = true)
    fun getGoalWithProgress(
        userId: UUID,
        goalId: UUID
    ): SavingsGoalProgress? {

        // Ищем цель только среди целей пользователя.
        val goal = goalService.getGoal(
            userId = userId,
            goalId = goalId
        ) ?: return null

        val totalSaved = savingsSummaryService
            .getConfirmedTotal(userId)

        return calculateProgress(goal, totalSaved)
    }

    private fun calculateProgress(
        goal: SavingsGoal,
        totalSaved: BigDecimal
    ): SavingsGoalProgress {

        require(goal.targetAmount > BigDecimal.ZERO) {
            "Сумма цели должна быть больше нуля"
        }

        val confirmedTotal = totalSaved.max(
            BigDecimal.ZERO
        )

        val achieved = confirmedTotal >= goal.targetAmount

        // Остаток не должен становиться отрицательным.
        val remainingAmount = goal.targetAmount
            .subtract(confirmedTotal)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)

        // Ограничиваем прогресс значением 100%.
        // Округляем вниз, чтобы цель не показывала
        // 100% до фактического достижения суммы.
        val progressPercent = if (achieved) {
            BigDecimal("100.00")
        } else {
            confirmedTotal
                .multiply(BigDecimal("100"))
                .divide(
                    goal.targetAmount,
                    2,
                    RoundingMode.DOWN
                )
        }

        return SavingsGoalProgress(
            goalId = goal.id,
            title = goal.title,
            presetId = goal.presetId,
            targetAmount = goal.targetAmount,
            totalSaved = confirmedTotal.setScale(
                2,
                RoundingMode.HALF_UP
            ),
            remainingAmount = remainingAmount,
            progressPercent = progressPercent,
            achieved = achieved
        )
    }
}
