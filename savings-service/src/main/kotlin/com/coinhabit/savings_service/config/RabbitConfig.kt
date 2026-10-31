
@file:Suppress("DEPRECATION", "removal")

package com.coinhabit.savings_service.config

import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.JacksonUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    companion object {
        const val EXCHANGE_NAME = "habit.events"
        const val QUEUE_NAME = "coinhabit.savings.days"
        const val ROUTING_KEY = "habit.day.passed"

        const val DEAD_LETTER_EXCHANGE = "coinhabit.savings.dlx"
        const val DEAD_LETTER_QUEUE = "coinhabit.savings.days.dlq"
        const val DEAD_LETTER_ROUTING_KEY = "coinhabit.savings.days.failed"
    }

    @Bean
    fun habitEventsExchange(): TopicExchange {
        return TopicExchange(
            EXCHANGE_NAME,
            true,
            false
        )
    }

    @Bean
    fun savingsDayQueue(): Queue {
        return QueueBuilder
            .durable(QUEUE_NAME)
            .build()
    }

    @Bean
    fun habitDayPassedBinding(
        savingsDayQueue: Queue,
        habitEventsExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(savingsDayQueue)
            .to(habitEventsExchange)
            .with(ROUTING_KEY)
    }

    @Bean
    fun savingsDeadLetterExchange(): DirectExchange {
        return DirectExchange(
            DEAD_LETTER_EXCHANGE,
            true,
            false
        )
    }

    @Bean
    fun savingsDeadLetterQueue(): Queue {
        return QueueBuilder
            .durable(DEAD_LETTER_QUEUE)
            .build()
    }

    @Bean
    fun savingsDeadLetterBinding(
        savingsDeadLetterQueue: Queue,
        savingsDeadLetterExchange: DirectExchange
    ): Binding {
        return BindingBuilder
            .bind(savingsDeadLetterQueue)
            .to(savingsDeadLetterExchange)
            .with(DEAD_LETTER_ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        val objectMapper = JacksonUtils
            .enhancedObjectMapper()

        objectMapper.disable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        )

        return Jackson2JsonMessageConverter(
            objectMapper
        )
    }
}
