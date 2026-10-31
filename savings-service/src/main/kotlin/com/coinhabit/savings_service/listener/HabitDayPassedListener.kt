
package com.coinhabit.savings_service.listener

import com.coinhabit.savings_service.config.RabbitConfig
import com.coinhabit.savings_service.event.HabitDayPassedEvent
import com.coinhabit.savings_service.repository.SavingsTransactionRepository
import com.coinhabit.savings_service.service.SavingsService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class HabitDayPassedListener(
    private val savingsService: SavingsService,
    private val savingsTransactionRepository: SavingsTransactionRepository
) {

    private val logger = LoggerFactory.getLogger(
        HabitDayPassedListener::class.java
    )

    @RabbitListener(queues = [RabbitConfig.QUEUE_NAME])
    fun handleHabitDayPassed(event: HabitDayPassedEvent) {

        logger.info(
            "Получен успешный день привычки: habitId={}, date={}, eventId={}",
            event.habitId,
            event.successfulDay,
            event.eventId
        )

        try {
            val transaction = savingsService.recordSuccessfulDay(
                eventId = event.eventId,
                habitId = event.habitId,
                userId = event.userId,
                successfulDay = event.successfulDay,
                monthlySpend = event.monthlySpend
            )

            if (transaction == null) {
                logger.info(
                    "Событие {} уже обработано. Повторное начисление пропущено.",
                    event.eventId
                )
                return
            }

            logger.info(
                "Начислена экономия: userId={}, habitId={}, amount={}, date={}",
                transaction.userId,
                transaction.habitId,
                transaction.savedAmount,
                transaction.successfulDay
            )

        } catch (exception: DataIntegrityViolationException) {

            // При одновременной обработке двух сообщений
            // одно из них может успеть сохранить транзакцию.
            // PostgreSQL отклонит вторую запись благодаря
            // уникальным ограничениям.

            val duplicateEvent =
                savingsTransactionRepository.existsByEventId(
                    event.eventId
                )

            val duplicateDay =
                savingsTransactionRepository
                    .existsByHabitIdAndSuccessfulDay(
                        event.habitId,
                        event.successfulDay
                    )

            if (duplicateEvent || duplicateDay) {
                logger.info(
                    "Транзакция для события {} или дня {} уже существует. " +
                            "Повторное начисление пропущено.",
                    event.eventId,
                    event.successfulDay
                )
                return
            }


            throw exception
        }
    }
}
