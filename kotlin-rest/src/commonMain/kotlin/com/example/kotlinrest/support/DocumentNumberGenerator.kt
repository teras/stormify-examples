package com.example.kotlinrest.support

/**
 * Centralizes document number generation so numbering rules are not scattered
 * across services and can later evolve without touching business logic.
 */
object DocumentNumberGenerator {
    fun nextPurchaseOrderNumber(): String = next("PO")
    fun nextSalesOrderNumber(): String = next("SO")
    fun nextShipmentNumber(): String = next("SHP")

    private fun next(prefix: String): String = "$prefix-${TimeSupport.compactTimestamp()}"
}
