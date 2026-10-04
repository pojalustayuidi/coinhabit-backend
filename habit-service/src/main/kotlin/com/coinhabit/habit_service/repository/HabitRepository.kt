package com.coinhabit.habit_service.repository

import com.coinhabit.habit_service.entity.Habit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface HabitRepository : JpaRepository<Habit, UUID> {
    fun findAllByUserId(userId: UUID): List<Habit>
}