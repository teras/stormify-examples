package com.example.kotlinrest.dto.inventory

import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.entity.StockItem
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
    val lastUpdatedAt: String?,
)

fun StockItem.toListItemResponse() = StockListItemResponse(
    id = pk(id),
    warehouseId = warehouse?.id,
    warehouseName = warehouse?.name,
    productId = product?.id,
    productSku = product?.sku,
    productName = product?.name,
    quantityOnHand = quantityOnHand,
    quantityReserved = quantityReserved,
    availableQuantity = quantityOnHand - quantityReserved,
    reorderLevel = product?.reorderLevel ?: 0,
    lastUpdatedAt = lastUpdatedAt,
)
