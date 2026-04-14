package com.example.kotlinrest.exception

/**
 * Domain-specific exceptions will later be translated into the shared
 * ErrorResponse DTO so the frontend receives predictable API errors.
 */
open class ApiException(
    message: String,
    val errorCode: String,
) : RuntimeException(message)
