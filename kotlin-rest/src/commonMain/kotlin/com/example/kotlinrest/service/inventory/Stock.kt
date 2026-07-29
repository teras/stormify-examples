package com.example.kotlinrest.service.inventory

import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.exception.ConflictException
import com.example.kotlinrest.support.now
import onl.ycode.stormify.executeUpdate

/**
 * Every movement of stock, expressed as a condition the database checks.
 *
 * The tempting shape is to read the row, decide in Kotlin whether there is enough, and
 * write the new number back. Two requests confirming the same order at the same moment
 * both read "120 available", both decide yes, and the second overwrites the first — the
 * warehouse has promised stock twice and nothing in the code looks wrong.
 *
 * So the check lives in the `WHERE` clause instead. The database evaluates it while it
 * holds the row, and reports how many rows it changed: zero means the condition did not
 * hold, and that is the only signal needed. No read, no window between deciding and
 * writing, and correct under concurrency without any locking of our own.
 */
internal object Stock {

    /** Goods arrived: they are on hand, and nobody has claimed them yet. */
    fun receive(warehouse: Warehouse, product: Product, quantity: Int) {
        ensureRow(warehouse, product)
        """
        UPDATE stock_item SET quantity_on_hand = quantity_on_hand + ?, last_updated_at = ?
        WHERE warehouse_id = ? AND product_id = ?
        """.trimIndent()
            .executeUpdate(quantity, now(), warehouse.id, product.id)
    }

    /**
     * Claim goods for an order. The condition is what makes this safe: only rows with
     * enough *unreserved* stock match, so a second concurrent reserve of the same units
     * changes nothing and is told so.
     */
    fun reserve(warehouse: Warehouse, product: Product, quantity: Int) {
        ensureRow(warehouse, product)
        val changed = """
        UPDATE stock_item SET quantity_reserved = quantity_reserved + ?, last_updated_at = ?
        WHERE warehouse_id = ? AND product_id = ? AND quantity_on_hand - quantity_reserved >= ?
        """.trimIndent()
            .executeUpdate(quantity, now(), warehouse.id, product.id, quantity)
        if (changed == 0)
            throw ConflictException("Insufficient available stock for product ${product.sku}")
    }

    /**
     * Goods left the building: they are neither on hand nor reserved any more. Both
     * counters must cover the quantity, or the row does not match and the shipment is
     * refused rather than driving a counter negative.
     */
    fun deduct(warehouse: Warehouse, product: Product, quantity: Int) {
        val changed = """
        UPDATE stock_item
        SET quantity_on_hand = quantity_on_hand - ?, quantity_reserved = quantity_reserved - ?,
            last_updated_at = ?
        WHERE warehouse_id = ? AND product_id = ? AND quantity_on_hand >= ? AND quantity_reserved >= ?
        """.trimIndent()
            .executeUpdate(quantity, quantity, now(), warehouse.id, product.id, quantity, quantity)
        if (changed == 0)
            throw ConflictException("Stock is inconsistent for product ${product.sku}")
    }

    /**
     * Makes sure the (warehouse, product) row exists without a read-then-insert race:
     * `UNIQUE (warehouse_id, product_id)` in the schema is what makes the conflict clause
     * meaningful, so two concurrent callers produce one row rather than two.
     */
    private fun ensureRow(warehouse: Warehouse, product: Product) {
        """
        INSERT INTO stock_item (warehouse_id, product_id, quantity_on_hand, quantity_reserved, last_updated_at)
        VALUES (?, ?, 0, 0, ?) ON CONFLICT DO NOTHING
        """.trimIndent()
            .executeUpdate(warehouse.id, product.id, now())
    }
}
