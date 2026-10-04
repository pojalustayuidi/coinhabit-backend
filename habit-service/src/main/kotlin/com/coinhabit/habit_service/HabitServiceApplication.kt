package com.coinhabit.habit_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HabitServiceApplication

fun main(args: Array<String>) {
	runApplication<HabitServiceApplication>(*args)
}
