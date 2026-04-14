package com.example.kotlinrest.dto.common

import kotlinx.serialization.Serializable

/**
 * A stable error contract makes frontend handling predictable and keeps both
 * backend implementations aligned on the same API shape.
 */
@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: String,
    val details: Map<String, String> = emptyMap(),
)
