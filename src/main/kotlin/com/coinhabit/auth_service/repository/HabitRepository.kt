package com.coinhabit.auth_service.repository

import com.coinhabit.auth_service.entity.Habit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HabitRepository : JpaRepository<Habit, Long>