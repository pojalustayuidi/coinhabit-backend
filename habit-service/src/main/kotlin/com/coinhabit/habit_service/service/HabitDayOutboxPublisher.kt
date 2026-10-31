
package com.coinhabit.habit_service.service

import com.coinhabit.habit_service.event.HabitDayPassedEvent
import com.coinhabit.habit_service.repository.HabitDayOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class HabitDayOutboxPublisher(
    private val outboxRepository: HabitDayOutboxRepository,
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(
        HabitDayOutboxPublisher::class.java
    )

    @Scheduled(
        fixedDelayString = "PT1M",
        initialDelayString = "PT10S"
    )
    fun publishPendingEvents() {

        val pendingEvents = outboxRepository
            .findTop100ByPublishedFalseOrderBySuccessfulDayAsc()

        for (record in pendingEvents) {

            try {
                val event = HabitDayPassedEvent(
                    eventId = record.id,
                    habitId = record.habitId,
                    userId = record.userId,
                    successfulDay = record.successfulDay,
                    currentStreakDays =
                        record.currentStreakDays
                )

                val correlationData = CorrelationData(
                    UUID.randomUUID().toString()
                )

                rabbitTemplate.convertAndSend(
                    "habit.events",
                    "habit.day.passed",
                    event,
                    correlationData
                )

                val confirm = correlationData.future.get(
                    10,
                    TimeUnit.SECONDS
                )

                if (!confirm.ack) {
                    logger.warn(
                        "RabbitMQ не подтвердил событие {}: {}",
                        record.id,
                        confirm.reason
                    )
                    continue
                }

                val returned = correlationData.returned

                if (returned != null) {
                    logger.warn(
                        "Событие {} не попало в очередь: {}",
                        record.id,
                        returned.replyText
                    )
                    continue
                }

                record.published = true
                outboxRepository.save(record)

                logger.info(
                    "Опубликован успешный день {} привычки {}",
                    record.successfulDay,
                    record.habitId
                )

            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()

                logger.warn(
                    "Публикация событий прервана",
                    exception
                )
                return

            } catch (exception: Exception) {
                logger.error(
                    "Не удалось опубликовать событие {}",
                    record.id,
                    exception
                )
            }
        }
    }
}
