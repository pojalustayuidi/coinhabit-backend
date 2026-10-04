package com.coinhabit.auth_service.dto

data class ErrorResponse(
    val status: Int,
    val message: String?,
    val timestamp: Long = System.currentTimeMillis()
)