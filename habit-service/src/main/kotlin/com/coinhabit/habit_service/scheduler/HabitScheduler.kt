
package com.coinhabit.habit_service.scheduler

import com.coinhabit.habit_service.entity.HabitDayOutbox
import com.coinhabit.habit_service.repository.HabitDayOutboxRepository
import com.coinhabit.habit_service.repository.HabitRepository
import com.coinhabit.habit_service.service.HabitDayPlanner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class HabitScheduler(
    private val habitRepository: HabitRepository,
    private val outboxRepository: HabitDayOutboxRepository
) {

    // Каждые пять минут по UTC.
    // После перезапуска можно восстановить
    // пропущенные завершённые дни.
    @Scheduled(
        cron = "0 */5 * * * *",
        zone = "UTC"
    )
    @Transactional
    fun processDailyHabits() {

        val zoneId = ZoneOffset.UTC
        val today = LocalDate.now(zoneId)

        val activeHabits =
            habitRepository.findAllByStatus("active")

        activeHabits.forEach { habit ->

            val habitId = requireNotNull(habit.id) {
                "У привычки отсутствует ID"
            }

            // Последний день, записанный в журнал.
            val lastRecordedDay = outboxRepository
                .findTopByHabitIdOrderBySuccessfulDayDesc(
                    habitId
                )
                ?.successfulDay

            // Получаем только недостающие дни.
            val daysToRecord = HabitDayPlanner
                .planCompletedDays(
                    startDate = habit.startDate,
                    lastRelapseAt = habit.lastRelapseAt,
                    today = today,
                    zoneId = zoneId,
                    lastRecordedDay = lastRecordedDay
                )

            if (daysToRecord.isEmpty()) {
                return@forEach
            }

            val records = daysToRecord.map { day ->

                // Один и тот же день всегда получает
                // одинаковый ID события.
                val eventKey =
                    "habit-day:$habitId:${day.successfulDay}"

                val eventId = UUID.nameUUIDFromBytes(
                    eventKey.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

                HabitDayOutbox(
                    id = eventId,
                    habitId = habitId,
                    userId = habit.userId,
                    successfulDay = day.successfulDay,
                    currentStreakDays =
                        day.currentStreakDays,
                    published = false
                )
            }

            outboxRepository.saveAll(records)
        }
    }
}
