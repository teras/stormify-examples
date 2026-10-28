package com.example.kotlinrest.db

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.mkdir

@OptIn(ExperimentalForeignApi::class)
internal actual fun createParentDirectories(filePath: String) {
    val parent = filePath.substringBeforeLast('/', "")
    // An absolute path keeps its leading '/', so the directories are created from the root.
    val current = StringBuilder(if (parent.startsWith('/')) "/" else "")
    for (segment in parent.split('/')) {
        if (segment.isEmpty() || segment == ".") continue
        if (current.isNotEmpty() && current.last() != '/') current.append('/')
        current.append(segment)
        mkdir(current.toString(), 0x1EDu) // 0755
    }
}
