
@file:Suppress("DEPRECATION", "removal")

package com.coinhabit.habit_service.config

import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.JacksonUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    @Bean
    fun habitEventsExchange(): TopicExchange {
        return TopicExchange("habit.events")
    }

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {

        val objectMapper = JacksonUtils
            .enhancedObjectMapper()

        // Даты отправляем строками ISO-8601:
        // "2026-10-30", а не [2026, 10, 30].
        objectMapper.disable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        )

        return Jackson2JsonMessageConverter(
            objectMapper
        )
    }
}
