package com.coinhabit.auth_service.exception

class UserAlreadyExistsException(message: String) : RuntimeException(message)

class InvalidCredentialsException(message: String) : RuntimeException(message)