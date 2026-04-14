package com.example.kotlinrest.dto.inventory

import kotlinx.serialization.Serializable

@Serializable
data class StockListItemResponse(
    val id: Int,
    val warehouseId: Int?,
    val warehouseName: String?,
    val productId: Int?,
    val productSku: String?,
    val productName: String?,
    val quantityOnHand: Int,
    val quantityReserved: Int,
    val availableQuantity: Int,
    val reorderLevel: Int,
    val lastUpdatedAt: String,
)
