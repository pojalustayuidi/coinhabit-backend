
package com.coinhabit.habit_service.scheduler

import com.coinhabit.habit_service.event.HabitDayPassedEvent
import com.coinhabit.habit_service.repository.HabitRepository
import com.coinhabit.habit_service.service.HabitStreakCalculator
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class HabitScheduler(
    private val habitRepository: HabitRepository,
    private val rabbitTemplate: RabbitTemplate
) {

    // Запускается ежедневно в полночь по UTC.
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional(readOnly = true)
    fun processDailyHabits() {
        val activeHabits =
            habitRepository.findAllByStatus("active")

        val zoneId = ZoneOffset.UTC
        val today = LocalDate.now(zoneId)

        activeHabits.forEach { habit ->

            val currentStreak = HabitStreakCalculator
                .calculateCurrentStreak(
                    startDate = habit.startDate,
                    lastRelapseAt = habit.lastRelapseAt,
                    today = today,
                    zoneId = zoneId
                )

            if (currentStreak == 0) {
                return@forEach
            }

            val event = HabitDayPassedEvent(
                habitId = requireNotNull(habit.id),
                userId = habit.userId,
                currentStreakDays = currentStreak
            )

            rabbitTemplate.convertAndSend(
                "habit.events",
                "habit.day.passed",
                event
            )
        }
    }
}
