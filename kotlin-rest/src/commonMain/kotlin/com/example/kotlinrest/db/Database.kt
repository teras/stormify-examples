package com.example.kotlinrest.db

import com.example.kotlinrest.config.AppSettings
import com.example.kotlinrest.exception.ConflictException
import onl.ycode.kdbc.KdbcDataSource
import onl.ycode.kdbc.SQLException
import onl.ycode.stormify.Stormify
import onl.ycode.stormify.generated.stormifyEntities

fun openDatabase(settings: AppSettings): Stormify {
    val parent = settings.dbPath.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) createDirectories(parent)
    // initSql runs these on every fresh connection, in order. Foreign keys are off by
    // default in SQLite, and a busy timeout lets a write wait for a concurrent one instead
    // of failing outright.
    val dataSource = KdbcDataSource(
        "jdbc:sqlite:${settings.dbPath}",
        initSql = "PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 5000",
    )
    return Stormify(dataSource, stormifyEntities).apply {
        logger.level = settings.logLevel
    }.asDefault()
}

/**
 * Turns a constraint the database rejected into the 409 it deserves, with a message the
 * caller can act on: [whenViolated] describes the operation, because "cannot delete" and
 * "already exists" are the same SQLite error seen from two directions.
 *
 * This is the only place in the example that reads a driver's error text. Constraint
 * violations are not distinguishable through a portable API, so the sniffing is confined
 * here rather than repeated at each call site — and anything that is *not* a constraint
 * violation is rethrown untouched, so real failures still surface as failures.
 */
internal inline fun <R> catchingConstraints(whenViolated: String, block: () -> R): R =
    try {
        block()
    } catch (e: SQLException) {
        val text = e.message ?: ""
        if (text.contains("constraint failed", ignoreCase = true) ||
            text.contains("constraint violation", ignoreCase = true)
        ) throw ConflictException(whenViolated)
        else throw e
    }
