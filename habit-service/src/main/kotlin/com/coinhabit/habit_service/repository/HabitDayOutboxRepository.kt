
package com.coinhabit.habit_service.repository

import com.coinhabit.habit_service.entity.HabitDayOutbox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface HabitDayOutboxRepository :
    JpaRepository<HabitDayOutbox, UUID> {

    fun findTopByHabitIdOrderBySuccessfulDayDesc(
        habitId: UUID
    ): HabitDayOutbox?

    fun findTop100ByPublishedFalseOrderBySuccessfulDayAsc():
            List<HabitDayOutbox>
}
