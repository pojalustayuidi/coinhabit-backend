package com.coinhabit.habit_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class HabitServiceApplication

fun main(args: Array<String>) {
	runApplication<HabitServiceApplication>(*args)
}