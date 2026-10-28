package com.example.kotlinrest.db

internal expect fun envOrNull(name: String): String?

internal expect fun createParentDirectories(filePath: String)
