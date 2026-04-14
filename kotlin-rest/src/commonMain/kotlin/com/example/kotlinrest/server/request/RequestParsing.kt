package com.example.kotlinrest.server.request

import com.example.kotlinrest.exception.ValidationException
import io.ktor.server.application.ApplicationCall

internal fun ApplicationCall.requireIntPath(name: String): Int {
    val value = parameters[name] ?: throw ValidationException("Missing path parameter '$name'")
    return value.toIntOrNull() ?: throw ValidationException("Path parameter '$name' must be an integer")
}
