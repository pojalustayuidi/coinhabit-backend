
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
                    currentStreakDays = record.currentStreakDays,

                    // Передаём стоимость привычки
                    // из сохранённого события Outbox.
                    monthlySpend = record.monthlySpend
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

                // Ожидаем подтверждение от RabbitMQ.
                val confirm = correlationData.future.get(
                    10,
                    TimeUnit.SECONDS
                )

                if (!confirm.ack) {
                    logger.warn(
                        "RabbitMQ не подтвердил событие {}",
                        record.id
                    )
                    continue
                }

                // Проверяем, не вернул ли RabbitMQ
                // сообщение из-за отсутствия маршрута.
                val returned = correlationData.returned

                if (returned != null) {
                    logger.warn(
                        "Событие {} не попало в очередь: {}",
                        record.id,
                        returned.replyText
                    )
                    continue
                }

                // Подтверждённое событие отмечаем
                // опубликованным в PostgreSQL.
                record.published = true
                outboxRepository.save(record)

                logger.info(
                    "Опубликован успешный день {} привычки {}, monthlySpend={}",
                    record.successfulDay,
                    record.habitId,
                    record.monthlySpend
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
