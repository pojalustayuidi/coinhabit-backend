
package com.coinhabit.savings_service.goal

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SavingsGoalRepository :
    JpaRepository<SavingsGoal, UUID> {

    // Все цели пользователя:
    // сначала самые новые.
    fun findAllByUserIdOrderByCreatedAtDesc(
        userId: UUID
    ): List<SavingsGoal>

    // Найти конкретную цель,
    // только если она принадлежит пользователю.
    fun findByIdAndUserId(
        id: UUID,
        userId: UUID
    ): SavingsGoal?

    // Проверить принадлежность цели пользователю.
    fun existsByIdAndUserId(
        id: UUID,
        userId: UUID
    ): Boolean

    // Удалить только собственную цель.
    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID
    ): Long
}
