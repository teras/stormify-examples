package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.dto.transaction.TransactionReferenceResponse
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.StockItem
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.support.TimeSupport
import onl.ycode.stormify.*

internal object ServiceSupport {
    fun now(): String = TimeSupport.nowIsoString()

    fun validateText(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }

    fun validatePositive(quantity: Int, label: String) {
        if (quantity <= 0) throw ValidationException("$label must be positive")
    }

    fun validateNonNegative(amount: Double, label: String) {
        if (amount < 0.0) throw ValidationException("$label must not be negative")
    }

    fun loadProduct(id: Int): Product =
        findById<Product>(id) ?: throw EntityNotFoundException("Product", id)

    fun loadWarehouse(id: Int): Warehouse =
        findById<Warehouse>(id) ?: throw EntityNotFoundException("Warehouse", id)

    fun loadOrCreateStockItem(warehouse: Warehouse, product: Product): StockItem {
        val existing = findAll<StockItem>(
            "WHERE warehouse_id = ? AND product_id = ?",
            warehouse.id,
            product.id,
        ).firstOrNull()
        return existing ?: StockItem().apply {
            this.warehouse = warehouse
            this.product = product
            quantityOnHand = 0
            quantityReserved = 0
            lastUpdatedAt = now()
        }.create()
    }

    fun ref(id: Int?, label: String?): TransactionReferenceResponse? =
        if (id == null || label == null) null else TransactionReferenceResponse(id, label)
}
