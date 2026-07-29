package com.example.kotlinrest.support

import onl.ycode.stormify.executeUpdate
import onl.ycode.stormify.readOne

/**
 * Hands out `PO-000123`-style numbers from a counter kept in the database.
 *
 * A millisecond timestamp is the obvious shortcut and it is wrong twice over: two orders
 * created in the same millisecond collide, and the numbers a business actually audits are
 * expected to run 1, 2, 3 with no holes. A counter row gives both — and because it is
 * bumped on the same connection as the insert that consumes it, an order that rolls back
 * takes its number back with it.
 *
 * `UPDATE` before `SELECT` is what makes it safe under concurrency: the update takes the
 * row lock, so a second caller waits there instead of reading a value that is about to
 * change underneath it.
 */
internal object DocumentNumberGenerator {
    fun nextPurchaseOrderNumber(): String = next("PO")
    fun nextSalesOrderNumber(): String = next("SO")
    fun nextShipmentNumber(): String = next("SHP")

    private fun next(prefix: String): String {
        "INSERT INTO doc_counter (prefix, next_value) VALUES (?, 1) ON CONFLICT DO NOTHING"
            .executeUpdate(prefix)
        "UPDATE doc_counter SET next_value = next_value + 1 WHERE prefix = ?".executeUpdate(prefix)
        val value = "SELECT next_value - 1 FROM doc_counter WHERE prefix = ?".readOne<Int>(prefix)
            ?: error("Counter for '$prefix' vanished mid-transaction")
        return "$prefix-${value.toString().padStart(6, '0')}"
    }
}
