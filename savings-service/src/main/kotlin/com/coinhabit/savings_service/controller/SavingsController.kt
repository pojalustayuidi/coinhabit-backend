
package com.coinhabit.savings_service.controller

import com.coinhabit.savings_service.service.SavingsSummaryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/savings")
class SavingsController(
    private val savingsSummaryService: SavingsSummaryService
) {

    @GetMapping("/me")
    fun getMySavings(
        @AuthenticationPrincipal userId: UUID
    ): SavingsSummaryResponse {

        // userId получаем из проверенного JWT,
        // а не из параметров HTTP-запроса.
        val totalSaved = savingsSummaryService
            .getConfirmedTotal(userId)

        return SavingsSummaryResponse(
            totalSaved = totalSaved
        )
    }
}

data class SavingsSummaryResponse(
    val totalSaved: BigDecimal
)
