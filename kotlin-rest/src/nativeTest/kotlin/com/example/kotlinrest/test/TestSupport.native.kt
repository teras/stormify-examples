package com.example.kotlinrest.test

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.remove

@OptIn(ExperimentalForeignApi::class)
internal actual fun removeFile(path: String) {
    remove(path)
}
