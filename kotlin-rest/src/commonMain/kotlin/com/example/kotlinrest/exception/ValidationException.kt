package com.example.kotlinrest.exception

class ValidationException(message: String) : ApiException(message, "VALIDATION_ERROR")
