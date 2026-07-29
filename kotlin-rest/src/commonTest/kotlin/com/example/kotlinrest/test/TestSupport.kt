package com.example.kotlinrest.test

import com.example.kotlinrest.config.AppSettings
import com.example.kotlinrest.db.applySchemaIfNeeded
import com.example.kotlinrest.db.openDatabase
import onl.ycode.logger.LogLevel
import onl.ycode.stormify.Stormify

/** Removes a file if it is there; a no-op if it is not. Backed per platform by the C runtime. */
internal expect fun removeFile(path: String)

/**
 * A freshly seeded database on its own file, so each test starts from the same known state.
 *
 * A file rather than `:memory:` on purpose: the connection pool pins each connection to its
 * own thread, and an in-memory SQLite database is private to the connection that opened it,
 * so a pooled read would see an empty database. The WAL side files are cleared too, or a
 * leftover journal would resurrect the previous run's rows.
 */
internal fun freshDatabase(name: String): Stormify {
    val path = "build/test-db/$name.db"
    removeFile(path)
    removeFile("$path-wal")
    removeFile("$path-shm")
    val stormify = openDatabase(AppSettings(dbPath = path, logLevel = LogLevel.ERROR))
    applySchemaIfNeeded(stormify)
    return stormify
}
