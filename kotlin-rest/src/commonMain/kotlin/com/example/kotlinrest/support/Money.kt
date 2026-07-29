package com.example.kotlinrest.support

/**
 * Money crosses the JSON wire as integer cents, and the client formats it for display. A CSV,
 * though, is opened directly by a person, so an amount is written here in major units with two
 * decimals — otherwise the export would read `64536` where the UI shows `645.36`.
 */
internal fun centsToDecimal(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = if (cents < 0) -cents else cents
    return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}
