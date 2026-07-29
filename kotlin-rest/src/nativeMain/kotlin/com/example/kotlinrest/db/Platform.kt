package com.example.kotlinrest.db

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun envOrNull(name: String): String? = getenv(name)?.toKString()
