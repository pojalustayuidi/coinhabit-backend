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
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class HabitService(
    private val habitRepository: HabitRepository
) {

    @Transactional
    fun createHabits(userId: UUID, requests: List<HabitRequest>) {
        val now = Instant.now()
        val habits = requests.map { req ->
            Habit(
                userId = userId,
                title = req.title,
                monthlySpend = req.monthlySpend,
                startDate = now,
                currentStreakDays = 0,
                status = "active"
            )
        }
        habitRepository.saveAll(habits)
    }

    @Transactional(readOnly = true)
    fun getUserHabits(userId: UUID): List<HabitResponse> {
        val habits = habitRepository.findAllByUserId(userId)
        val now = Instant.now()

        return habits.map { habit ->
            val actualStreak = ChronoUnit.DAYS.between(habit.startDate, now).toInt()

            val dailySpend = habit.monthlySpend.divide(BigDecimal(30), 2, RoundingMode.HALF_UP)
            val savedAmount = dailySpend.multiply(BigDecimal(actualStreak))

            HabitResponse(
                id = habit.id!!,
                title = habit.title,
                monthlySpend = habit.monthlySpend,
                startDate = habit.startDate,
                currentStreakDays = actualStreak,
                status = habit.status,
                lastRelapseAt = habit.lastRelapseAt,
                savedAmount = savedAmount
            )
        }
    }

    @Transactional
    fun relapseHabit(habitId: UUID, userId: UUID) {
        val habit = habitRepository.findById(habitId)
            .orElseThrow { IllegalArgumentException("Привычка не найдена") }

        // Проверка владения привычкой (важное требование безопасности)
        if (habit.userId != userId) {
            throw SecurityException("Нет прав на сброс этой привычки")
        }

        val now = Instant.now()
        habit.currentStreakDays = 0
        habit.startDate = now
        habit.lastRelapseAt = now
        habit.status = "relapsed"

        habitRepository.save(habit)

        // TODO: В будущем добавить Scheduled Job, которая раз в сутки проходит по активным привычкам
        // и публикует событие HabitDayPassed в RabbitMQ для Savings/Gamification сервисов.
        // Здесь же (при срыве) будет публиковаться событие HabitRelapsed.
    }
}