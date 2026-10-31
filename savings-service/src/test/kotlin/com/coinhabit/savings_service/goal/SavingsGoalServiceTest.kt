
package com.coinhabit.savings_service.goal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SavingsGoalServiceTest {

    private val repository = mock(
        SavingsGoalRepository::class.java
    )

    private val service = SavingsGoalService(repository)

    private val userId = UUID.randomUUID()

    @Test
    fun `creates goal from preset`() {

        stubSave()

        val goal = service.createGoal(
            userId = userId,
            title = " Новые наушники ",
            targetAmount = BigDecimal("3000"),
            presetId = "headphones"
        )

        assertNotNull(goal.id)
        assertEquals(userId, goal.userId)
        assertEquals("Новые наушники", goal.title)
        assertEquals(
            BigDecimal("3000.00"),
            goal.targetAmount
        )
        assertEquals("headphones", goal.presetId)
    }

    @Test
    fun `creates custom goal without preset`() {

        stubSave()

        val goal = service.createGoal(
            userId = userId,
            title = "Новый велосипед",
            targetAmount = BigDecimal("25000.50")
        )

        assertEquals("Новый велосипед", goal.title)
        assertEquals(
            BigDecimal("25000.50"),
            goal.targetAmount
        )
        assertNull(goal.presetId)
    }

    @Test
    fun `rejects empty goal title`() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.createGoal(
                userId = userId,
                title = "   ",
                targetAmount = BigDecimal("3000")
            )
        }
    }

    @Test
    fun `rejects title longer than 120 characters`() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.createGoal(
                userId = userId,
                title = "А".repeat(121),
                targetAmount = BigDecimal("3000")
            )
        }
    }

    @Test
    fun `rejects zero target amount`() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.createGoal(
                userId = userId,
                title = "Наушники",
                targetAmount = BigDecimal.ZERO
            )
        }
    }

    @Test
    fun `rejects negative target amount`() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.createGoal(
                userId = userId,
                title = "Наушники",
                targetAmount = BigDecimal("-100")
            )
        }
    }

    @Test
    fun `rejects unknown preset`() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.createGoal(
                userId = userId,
                title = "Наушники",
                targetAmount = BigDecimal("3000"),
                presetId = "unknown"
            )
        }
    }

    @Test
    fun `returns goals belonging to user`() {

        `when`(
            repository.findAllByUserIdOrderByCreatedAtDesc(
                userId
            )
        ).thenReturn(emptyList())

        val goals = service.getGoals(userId)

        assertTrue(goals.isEmpty())

        verify(repository)
            .findAllByUserIdOrderByCreatedAtDesc(userId)
    }

    @Test
    fun `does not find goal belonging to another user`() {

        val goalId = UUID.randomUUID()

        `when`(
            repository.findByIdAndUserId(
                goalId,
                userId
            )
        ).thenReturn(null)

        val goal = service.getGoal(
            userId = userId,
            goalId = goalId
        )

        assertNull(goal)

        verify(repository).findByIdAndUserId(
            goalId,
            userId
        )
    }

    @Test
    fun `deletes goal belonging to user`() {

        val goalId = UUID.randomUUID()

        `when`(
            repository.deleteByIdAndUserId(
                goalId,
                userId
            )
        ).thenReturn(1L)

        val deleted = service.deleteGoal(
            userId = userId,
            goalId = goalId
        )

        assertTrue(deleted)
    }

    @Test
    fun `returns false when goal cannot be deleted`() {

        val goalId = UUID.randomUUID()

        `when`(
            repository.deleteByIdAndUserId(
                goalId,
                userId
            )
        ).thenReturn(0L)

        val deleted = service.deleteGoal(
            userId = userId,
            goalId = goalId
        )

        assertFalse(deleted)
    }

    // НОВЫЕ ТЕСТЫ РЕДАКТИРОВАНИЯ

    @Test
    fun `updates own goal title and amount`() {

        val goal = createExistingGoal()

        `when`(
            repository.findByIdAndUserId(
                goal.id,
                userId
            )
        ).thenReturn(goal)

        stubSave()

        val updated = service.updateGoal(
            userId = userId,
            goalId = goal.id,
            title = " Игровые наушники ",
            targetAmount = BigDecimal("5500")
        )

        assertNotNull(updated)
        assertEquals("Игровые наушники", updated?.title)
        assertEquals(
            BigDecimal("5500.00"),
            updated?.targetAmount
        )

        // ID, владелец и шаблон не меняются.
        assertEquals(goal.id, updated?.id)
        assertEquals(userId, updated?.userId)
        assertEquals("headphones", updated?.presetId)

        verify(repository).save(goal)
    }

    @Test
    fun `cannot update goal belonging to another user`() {

        val goal = createExistingGoal()
        val anotherUserId = UUID.randomUUID()

        `when`(
            repository.findByIdAndUserId(
                goal.id,
                anotherUserId
            )
        ).thenReturn(null)

        val updated = service.updateGoal(
            userId = anotherUserId,
            goalId = goal.id,
            title = "Чужая цель",
            targetAmount = BigDecimal("100")
        )

        assertNull(updated)

        // Исходная цель осталась без изменений.
        assertEquals("Новые наушники", goal.title)
        assertEquals(
            BigDecimal("3000.00"),
            goal.targetAmount
        )

        verify(repository, never()).save(
            any(SavingsGoal::class.java)
        )
    }

    @Test
    fun `rejects empty title when updating goal`() {

        val goal = createExistingGoal()

        `when`(
            repository.findByIdAndUserId(
                goal.id,
                userId
            )
        ).thenReturn(goal)

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.updateGoal(
                userId = userId,
                goalId = goal.id,
                title = "   ",
                targetAmount = BigDecimal("5500")
            )
        }

        assertEquals("Новые наушники", goal.title)

        verify(repository, never()).save(
            any(SavingsGoal::class.java)
        )
    }

    @Test
    fun `rejects negative amount when updating goal`() {

        val goal = createExistingGoal()

        `when`(
            repository.findByIdAndUserId(
                goal.id,
                userId
            )
        ).thenReturn(goal)

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.updateGoal(
                userId = userId,
                goalId = goal.id,
                title = "Новые наушники",
                targetAmount = BigDecimal("-500")
            )
        }

        assertEquals(
            BigDecimal("3000.00"),
            goal.targetAmount
        )

        verify(repository, never()).save(
            any(SavingsGoal::class.java)
        )
    }

    @Test
    fun `rounds updated amount to two decimal places`() {

        val goal = createExistingGoal()

        `when`(
            repository.findByIdAndUserId(
                goal.id,
                userId
            )
        ).thenReturn(goal)

        stubSave()

        val updated = service.updateGoal(
            userId = userId,
            goalId = goal.id,
            title = "Новые наушники",
            targetAmount = BigDecimal("5500.555")
        )

        assertEquals(
            BigDecimal("5500.56"),
            updated?.targetAmount
        )
    }

    private fun createExistingGoal(): SavingsGoal {

        return SavingsGoal(
            id = UUID.randomUUID(),
            userId = userId,
            title = "Новые наушники",
            targetAmount = BigDecimal("3000.00"),
            presetId = "headphones"
        )
    }

    private fun stubSave() {

        doAnswer { invocation ->
            invocation.getArgument<SavingsGoal>(0)
        }.`when`(repository).save(
            any(SavingsGoal::class.java)
        )
    }
}
