package com.coinhabit.habit_service.scheduler

import com.coinhabit.habit_service.event.HabitDayPassedEvent
import com.coinhabit.habit_service.repository.HabitRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class HabitScheduler(
    private val habitRepository: HabitRepository,
    private val rabbitTemplate: RabbitTemplate
) {

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(readOnly = true)
    fun processDailyHabits() {
        val activeHabits = habitRepository.findAllByStatus("active")
        val now = Instant.now()

        activeHabits.forEach { habit ->
            val actualStreak = ChronoUnit.DAYS.between(habit.startDate, now).toInt()

            val event = HabitDayPassedEvent(
                habitId = habit.id!!,
                userId = habit.userId,
                currentStreakDays = actualStreak
            )

            rabbitTemplate.convertAndSend("habit.events", "habit.day.passed", event)
        }
    }
}