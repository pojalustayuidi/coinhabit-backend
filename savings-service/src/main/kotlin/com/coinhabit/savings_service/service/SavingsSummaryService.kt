
package com.coinhabit.savings_service.service

import com.coinhabit.savings_service.repository.SavingsTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class SavingsSummaryService(
    private val savingsTransactionRepository: SavingsTransactionRepository
) {

    @Transactional(readOnly = true)
    fun getConfirmedTotal(userId: UUID): BigDecimal {

        val totalSaved = savingsTransactionRepository
            .getTotalSavedByUserId(userId)


        return totalSaved.setScale(2)
    }
}
