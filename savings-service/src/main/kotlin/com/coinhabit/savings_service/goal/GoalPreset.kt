
package com.coinhabit.savings_service.goal

import java.math.BigDecimal

data class GoalPreset(
    val id: String,
    val title: String,
    val suggestedAmount: BigDecimal
)

object GoalPresetCatalog {

    val presets: List<GoalPreset> = listOf(
        GoalPreset(
            id = "headphones",
            title = "Новые наушники",
            suggestedAmount = BigDecimal("3000.00")
        ),
        GoalPreset(
            id = "smartwatch",
            title = "Смарт-часы",
            suggestedAmount = BigDecimal("8000.00")
        ),
        GoalPreset(
            id = "sneakers",
            title = "Новые кроссовки",
            suggestedAmount = BigDecimal("6000.00")
        ),
        GoalPreset(
            id = "smartphone",
            title = "Новый смартфон",
            suggestedAmount = BigDecimal("30000.00")
        ),
        GoalPreset(
            id = "vacuum",
            title = "Пылесос",
            suggestedAmount = BigDecimal("15000.00")
        ),
        GoalPreset(
            id = "console",
            title = "Игровая приставка",
            suggestedAmount = BigDecimal("45000.00")
        ),
        GoalPreset(
            id = "travel",
            title = "Путешествие",
            suggestedAmount = BigDecimal("80000.00")
        )
    )

    /*
     * TODO: Цели по ссылке на товар.
     *
     * Идея для будущей версии CoinHabit:
     *
     * Пользователь вставляет ссылку на товар
     * из интернет-магазина.
     *
     * Приложение по возможности автоматически
     * получает название, фотографию и цену.
     *
     * Например:
     * Ссылка на пылесос -> создаётся цель
     * с фотографией пылесоса и его стоимостью.
     *
     * Пользователь сможет изменить цену вручную.
     *
     * Реализовать позже:
     * - безопасную проверку ссылок;
     * - получение данных о товаре;
     * - обработку магазинов, где данные недоступны.
     *
     * Пока эту функцию НЕ реализуем.
     */
}
