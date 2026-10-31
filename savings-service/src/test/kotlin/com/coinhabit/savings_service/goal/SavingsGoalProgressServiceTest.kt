
package com.coinhabit.savings_service.goal

import com.coinhabit.savings_service.service.SavingsSummaryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SavingsGoalProgressServiceTest {

    private val goalService = mock(
        SavingsGoalService::class.java
    )

    private val savingsSummaryService = mock(
        SavingsSummaryService::class.java
    )

    private val service = SavingsGoalProgressService(
        goalService,
        savingsSummaryService
    )

    private val userId = UUID.randomUUID()

    @Test
    fun `calculates progress for headphones goal`() {

        val goal = createGoal(
            title = "Новые наушники",
            amount = "3000.00"
        )

        `when`(
            goalService.getGoals(userId)
        ).thenReturn(listOf(goal))

        `when`(
            savingsSummaryService.getConfirmedTotal(userId)
        ).thenReturn(BigDecimal("1200.00"))

        val result = service.getGoalsWithProgress(userId)

        assertEquals(1, result.size)

        val progress = result.first()

        assertEquals("Новые наушники", progress.title)
        assertEquals(
            BigDecimal("1200.00"),
            progress.totalSaved
        )
        assertEquals(
            BigDecimal("1800.00"),
            progress.remainingAmount
        )
        assertEquals(
            BigDecimal("40.00"),
            progress.progressPercent
        )
        assertFalse(progress.achieved)
    }

    @Test
    fun `marks goal as achieved when savings are enough`() {

        val goal = createGoal(
            title = "Новая книга",
            amount = "1000.00"
        )

        `when`(
            goalService.getGoals(userId)
        ).thenReturn(listOf(goal))

        `when`(
            savingsSummaryService.getConfirmedTotal(userId)
        ).thenReturn(BigDecimal("1200.00"))

        val progress = service
            .getGoalsWithProgress(userId)
            .first()

        assertTrue(progress.achieved)

        assertEquals(
            BigDecimal("100.00"),
            progress.progressPercent
        )

        assertEquals(
            BigDecimal("0.00"),
            progress.remainingAmount
        )
    }

    @Test
    fun `returns zero progress without savings`() {

        val goal = createGoal(
            title = "Смарт-часы",
            amount = "8000.00"
        )

        `when`(
            goalService.getGoals(userId)
        ).thenReturn(listOf(goal))

        `when`(
            savingsSummaryService.getConfirmedTotal(userId)
        ).thenReturn(BigDecimal.ZERO)

        val progress = service
            .getGoalsWithProgress(userId)
            .first()

        assertEquals(
            BigDecimal("0.00"),
            progress.progressPercent
        )

        assertEquals(
            BigDecimal("8000.00"),
            progress.remainingAmount
        )

        assertFalse(progress.achieved)
    }

    @Test
    fun `loads total savings only once for multiple goals`() {

        val headphones = createGoal(
            title = "Наушники",
            amount = "3000.00"
        )

        val sneakers = createGoal(
            title = "Кроссовки",
            amount = "6000.00"
        )

        `when`(
            goalService.getGoals(userId)
        ).thenReturn(
            listOf(headphones, sneakers)
        )

        `when`(
            savingsSummaryService.getConfirmedTotal(userId)
        ).thenReturn(BigDecimal("1200.00"))

        val result = service.getGoalsWithProgress(userId)

        assertEquals(2, result.size)

        assertEquals(
            BigDecimal("40.00"),
            result[0].progressPercent
        )

        assertEquals(
            BigDecimal("20.00"),
            result[1].progressPercent
        )

        verify(
            savingsSummaryService,
            times(1)
        ).getConfirmedTotal(userId)
    }

    @Test
    fun `does not query savings when there are no goals`() {

        `when`(
            goalService.getGoals(userId)
        ).thenReturn(emptyList())

        val result = service.getGoalsWithProgress(userId)

        assertTrue(result.isEmpty())

        verifyNoInteractions(savingsSummaryService)
    }

    @Test
    fun `does not calculate progress for inaccessible goal`() {

        val goalId = UUID.randomUUID()

        `when`(
            goalService.getGoal(userId, goalId)
        ).thenReturn(null)

        val result = service.getGoalWithProgress(
            userId = userId,
            goalId = goalId
        )

        assertNull(result)

        verify(goalService).getGoal(
            userId,
            goalId
        )

        verifyNoInteractions(savingsSummaryService)
    }

    private fun createGoal(
        title: String,
        amount: String
    ): SavingsGoal {

        return SavingsGoal(
            id = UUID.randomUUID(),
            userId = userId,
            title = title,
            targetAmount = BigDecimal(amount)
        )
    }
}
