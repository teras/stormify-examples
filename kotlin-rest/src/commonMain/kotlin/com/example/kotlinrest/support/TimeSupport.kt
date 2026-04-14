@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.kotlinrest.support

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Centralizes application-owned timestamps so the demo does not depend on SQL
 * dialect-specific date/time functions.
 */
object TimeSupport {
    fun nowIsoString(): String = Clock.System.now().toString()

    fun compactTimestamp(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val fraction = now.nanosecond / 1_000_000
        return buildString {
            append(now.year.toString().padStart(4, '0'))
            append(now.monthNumber.toString().padStart(2, '0'))
            append(now.dayOfMonth.toString().padStart(2, '0'))
            append(now.hour.toString().padStart(2, '0'))
            append(now.minute.toString().padStart(2, '0'))
            append(now.second.toString().padStart(2, '0'))
            append(fraction.toString().padStart(3, '0'))
        }
    }
}
