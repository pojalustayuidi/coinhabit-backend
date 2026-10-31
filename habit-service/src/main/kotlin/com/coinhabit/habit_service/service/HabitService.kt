
package com.coinhabit.habit_service.service

import com.coinhabit.habit_service.dto.HabitRequest
import com.coinhabit.habit_service.dto.HabitResponse
import com.coinhabit.habit_service.entity.Habit
import com.coinhabit.habit_service.repository.HabitRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.NoSuchElementException
import java.util.UUID

@Service
class HabitService(
    private val habitRepository: HabitRepository
) {

    @Transactional
    fun createHabits(
        userId: UUID,
        requests: List<HabitRequest>
    ) {
        val now = Instant.now()

        val habits = requests.map { request ->
            Habit(
                userId = userId,
                title = request.title,
                monthlySpend = request.monthlySpend,
                startDate = now,
                currentStreakDays = 0,
                status = "active",
                completedDaysBeforeCurrentStreak = 0
            )
        }

        habitRepository.saveAll(habits)
    }

    @Transactional(readOnly = true)
    fun getUserHabits(userId: UUID): List<HabitResponse> {
        val habits = habitRepository.findAllByUserId(userId)

        val zoneId = ZoneOffset.UTC
        val today = LocalDate.now(zoneId)

        return habits.map { habit ->

            val currentStreak = if (habit.status == "archived") {
                habit.currentStreakDays
            } else {
                HabitStreakCalculator.calculateCurrentStreak(
                    startDate = habit.startDate,
                    lastRelapseAt = habit.lastRelapseAt,
                    today = today,
                    zoneId = zoneId
                )
            }

            val totalDays = HabitStreakCalculator
                .calculateTotalDays(
                    completedDaysBeforeCurrentStreak =
                        habit.completedDaysBeforeCurrentStreak,
                    currentStreakDays = currentStreak
                )

            // Временная оценка экономии.
            // Подтверждённая сумма хранится в Savings Service.
            val savedAmount = habit.monthlySpend
                .multiply(BigDecimal.valueOf(totalDays.toLong()))
                .divide(
                    BigDecimal("30"),
                    2,
                    RoundingMode.HALF_UP
                )

            HabitResponse(
                id = requireNotNull(habit.id) {
                    "У привычки отсутствует ID"
                },
                title = habit.title,
                monthlySpend = habit.monthlySpend,
                startDate = habit.startDate,
                currentStreakDays = currentStreak,
                status = habit.status,
                lastRelapseAt = habit.lastRelapseAt,
                savedAmount = savedAmount
            )
        }
    }

    @Transactional
    fun updateHabit(
        habitId: UUID,
        userId: UUID,
        request: HabitRequest
    ) {
        val habit = habitRepository.findById(habitId)
            .orElseThrow {
                NoSuchElementException("Привычка не найдена")
            }

        if (habit.userId != userId) {
            throw SecurityException(
                "Нет прав на редактирование этой привычки"
            )
        }

        require(habit.status == "active") {
            "Нельзя редактировать архивную привычку"
        }

        val normalizedTitle = request.title.trim()

        require(normalizedTitle.isNotBlank()) {
            "Название привычки не может быть пустым"
        }

        require(normalizedTitle.length <= 120) {
            "Название привычки не может быть длиннее 120 символов"
        }

        val normalizedAmount = request.monthlySpend
            .setScale(2, RoundingMode.HALF_UP)

        require(normalizedAmount > BigDecimal.ZERO) {
            "Сумма трат должна быть больше нуля"
        }

        require(normalizedAmount.precision() <= 10) {
            "Сумма трат слишком большая"
        }

        habit.title = normalizedTitle
        habit.monthlySpend = normalizedAmount

        habitRepository.save(habit)
    }

    @Transactional
    fun archiveHabit(
        habitId: UUID,
        userId: UUID
    ) {
        val habit = habitRepository.findById(habitId)
            .orElseThrow {
                NoSuchElementException("Привычка не найдена")
            }

        if (habit.userId != userId) {
            throw SecurityException(
                "Нет прав на архивирование этой привычки"
            )
        }

        // Повторное архивирование ничего не меняет.
        if (habit.status == "archived") {
            return
        }

        require(habit.status == "active") {
            "Можно архивировать только активную привычку"
        }

        val now = Instant.now()
        val today = LocalDate.ofInstant(now, ZoneOffset.UTC)

        habit.currentStreakDays =
            HabitStreakCalculator.calculateCurrentStreak(
                startDate = habit.startDate,
                lastRelapseAt = habit.lastRelapseAt,
                today = today,
                zoneId = ZoneOffset.UTC
            )

        habit.status = "archived"

        habitRepository.save(habit)
    }

    @Transactional
    fun relapseHabit(
        habitId: UUID,
        userId: UUID
    ) {
        val habit = habitRepository.findById(habitId)
            .orElseThrow {
                NoSuchElementException("Привычка не найдена")
            }

        if (habit.userId != userId) {
            throw SecurityException(
                "Нет прав на сброс этой привычки"
            )
        }

        require(habit.status == "active") {
            "Нельзя сбросить архивную привычку"
        }

        val now = Instant.now()
        val zoneId = ZoneOffset.UTC
        val today = LocalDate.ofInstant(now, zoneId)

        val completedCurrentStreak = HabitStreakCalculator
            .calculateCurrentStreak(
                startDate = habit.startDate,
                lastRelapseAt = habit.lastRelapseAt,
                today = today,
                zoneId = zoneId
            )

        habit.completedDaysBeforeCurrentStreak =
            HabitStreakCalculator.calculateTotalDays(
                completedDaysBeforeCurrentStreak =
                    habit.completedDaysBeforeCurrentStreak,
                currentStreakDays = completedCurrentStreak
            )

        habit.currentStreakDays = 0
        habit.lastRelapseAt = now
        habit.status = "active"

        habitRepository.save(habit)
    }
}
