package com.coinhabit.habit_service.exception

import com.coinhabit.habit_service.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(e: IllegalArgumentException): ErrorResponse {
        return ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.message)
    }

    @ExceptionHandler(SecurityException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleSecurityException(e: SecurityException): ErrorResponse {
        return ErrorResponse(HttpStatus.FORBIDDEN.value(), e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ErrorResponse {
        val errors = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ErrorResponse(HttpStatus.BAD_REQUEST.value(), errors)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(e: Exception): ErrorResponse {
        e.printStackTrace() // Вывод деталей ошибки в консоль IDE
        return ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Внутренняя ошибка сервера")
    }
}