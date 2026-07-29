package com.example.kotlinrest.db

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.mkdir

@OptIn(ExperimentalForeignApi::class)
internal actual fun createDirectories(path: String) {
    val current = StringBuilder()
    for (segment in path.split('/')) {
        if (segment.isEmpty() || segment == ".") continue
        if (current.isNotEmpty()) current.append('/')
        current.append(segment)
        mkdir(current.toString()) // Windows mkdir takes no mode argument
    }
}
