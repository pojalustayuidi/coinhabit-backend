
package com.coinhabit.habit_service.service

import com.coinhabit.habit_service.dto.HabitRequest
import com.coinhabit.habit_service.dto.HabitResponse
import com.coinhabit.habit_service.entity.Habit
import com.coinhabit.habit_service.event.HabitRelapsedEvent
import com.coinhabit.habit_service.repository.HabitRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class HabitService(
    private val habitRepository: HabitRepository,
    private val rabbitTemplate: RabbitTemplate
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
                status = "active"
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

            val actualStreak = HabitStreakCalculator.calculate(
                startDate = habit.startDate,
                today = today,
                zoneId = zoneId
            )

            val dailySpend = habit.monthlySpend.divide(
                BigDecimal(30),
                2,
                RoundingMode.HALF_UP
            )

            val savedAmount = dailySpend.multiply(
                BigDecimal(actualStreak)
            )

            HabitResponse(
                id = requireNotNull(habit.id) {
                    "У привычки отсутствует ID"
                },
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
    fun relapseHabit(
        habitId: UUID,
        userId: UUID
    ) {
        val habit = habitRepository.findById(habitId)
            .orElseThrow {
                IllegalArgumentException("Привычка не найдена")
            }

        if (habit.userId != userId) {
            throw SecurityException(
                "Нет прав на сброс этой привычки"
            )
        }

        val now = Instant.now()

        habit.currentStreakDays = 0
        habit.startDate = now
        habit.lastRelapseAt = now
        habit.status = "relapsed"

        habitRepository.save(habit)

        val event = HabitRelapsedEvent(
            habitId = habitId,
            userId = userId
        )

        rabbitTemplate.convertAndSend(
            "habit.events",
            "habit.relapsed",
            event
        )
    }
}
