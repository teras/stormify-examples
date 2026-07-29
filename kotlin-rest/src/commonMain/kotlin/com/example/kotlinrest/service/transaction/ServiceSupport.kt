package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.support.now
import com.example.kotlinrest.support.isValidInstant
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.exception.ConflictException
import com.example.kotlinrest.exception.ReferenceNotFoundException
import com.example.kotlinrest.exception.ValidationException
import onl.ycode.stormify.*

internal object ServiceSupport {
    /**
     * Guards a state transition: the entity must be in [expected] or the operation is refused
     * with a 409. One helper keeps the six status checks — and their messages — uniform, and
     * names the state that *is* allowed rather than the one that was refused, so a status added
     * later is rejected by default instead of slipping through.
     */
    fun requireStatus(actual: Enum<*>, expected: Enum<*>, entities: String, action: String) {
        if (actual != expected)
            throw ConflictException("Only ${expected.name.lowercase()} $entities can be $action")
    }

    fun validateText(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }

    fun validatePositive(quantity: Int, label: String) {
        if (quantity <= 0) throw ValidationException("$label must be positive")
    }

    fun validateNonNegative(amount: Long, label: String) {
        if (amount < 0L) throw ValidationException("$label must not be negative")
    }

    /** An optional timestamp: absent is fine, but a present one must parse, or it is a 400. */
    fun validateOptionalInstant(value: String?, label: String, field: String) {
        if (!value.isNullOrBlank() && !isValidInstant(value))
            throw ValidationException("$label must be a valid ISO-8601 timestamp", field)
    }

    fun loadProduct(id: Int): Product =
        findById<Product>(id) ?: throw ReferenceNotFoundException("Product", "productId", id)

    fun loadWarehouse(id: Int): Warehouse =
        findById<Warehouse>(id) ?: throw ReferenceNotFoundException("Warehouse", "warehouseId", id)
}
