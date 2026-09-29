package com.coinhabit.auth_service.service

import com.coinhabit.auth_service.dto.HabitRequest
import com.coinhabit.auth_service.entity.Habit
import com.coinhabit.auth_service.repository.HabitRepository
import com.coinhabit.auth_service.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class HabitService(
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository
) {

    @Transactional
    fun saveHabits(email: String, requests: List<HabitRequest>) {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Пользователь не найден")

        val now = Instant.now()

        val habitsToSave = requests.map { request ->
            Habit(
                title = request.title,
                monthlySpend = request.monthlySpend,
                startDate = now,
                user = user //
            )
        }

        habitRepository.saveAll(habitsToSave)
    }
}