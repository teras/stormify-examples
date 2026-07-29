@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.kotlinrest.support

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

internal fun Instant.toFixedIsoString(): String {
    val dt = toLocalDateTime(TimeZone.UTC)
    return buildString {
        append(dt.year.toString().padStart(4, '0'))
        append('-')
        append(dt.monthNumber.toString().padStart(2, '0'))
        append('-')
        append(dt.dayOfMonth.toString().padStart(2, '0'))
        append('T')
        append(dt.hour.toString().padStart(2, '0'))
        append(':')
        append(dt.minute.toString().padStart(2, '0'))
        append(':')
        append(dt.second.toString().padStart(2, '0'))
        append('.')
        append((dt.nanosecond / 1_000_000).toString().padStart(3, '0'))
        append('Z')
    }
}

internal fun now(): String = Clock.System.now().toFixedIsoString()

/**
 * Whether [value] is an ISO-8601 instant the API would accept. A client-supplied timestamp is
 * stored verbatim, so it is checked here rather than trusted — an unparseable string becomes a
 * 400 at the service boundary instead of a row no reader can interpret.
 */
internal fun isValidInstant(value: String): Boolean =
    try {
        Instant.parse(value)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
