package com.example.kotlinrest.db

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.mkdir

@OptIn(ExperimentalForeignApi::class)
internal actual fun createParentDirectories(filePath: String) {
    // Windows accepts both separators; work with '/' only.
    var parent = filePath.replace('\\', '/').substringBeforeLast('/', "")
    val current = StringBuilder()
    // A drive prefix such as "C:" is kept as is, it is not a directory to create.
    if (parent.length >= 2 && parent[1] == ':') {
        current.append(parent, 0, 2)
        parent = parent.substring(2)
    }
    // An absolute path keeps its leading '/', so the directories are created from the root.
    if (parent.startsWith('/')) current.append('/')
    for (segment in parent.split('/')) {
        if (segment.isEmpty() || segment == ".") continue
        if (current.isNotEmpty() && current.last() != '/' && current.last() != ':') current.append('/')
        current.append(segment)
        mkdir(current.toString()) // Windows mkdir takes no mode argument
    }
}
