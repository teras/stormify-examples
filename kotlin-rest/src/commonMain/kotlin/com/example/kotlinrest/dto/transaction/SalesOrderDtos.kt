package com.example.kotlinrest.dto.transaction

import com.example.kotlinrest.entity.SalesOrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class SalesOrderItemInput(
    val productId: Int,
    val quantity: Int,
    val unitPrice: Double,
)

@Serializable
data class CreateSalesOrderRequest(
    val customerId: Int,
    val warehouseId: Int,
    val notes: String,
    val items: List<SalesOrderItemInput>,
)

@Serializable
data class UpdateSalesOrderRequest(
    val customerId: Int,
    val warehouseId: Int,
    val notes: String,
    val items: List<SalesOrderItemInput>,
)

@Serializable
data class SalesOrderItemResponse(
    val id: Int,
    val productId: Int?,
    val productSku: String?,
    val productName: String?,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
)

@Serializable
data class SalesOrderListItemResponse(
    val id: Int,
    val orderNumber: String,
    val customerId: Int?,
    val customerName: String?,
    val warehouseId: Int?,
    val warehouseName: String?,
    val status: SalesOrderStatus,
    val orderedAt: String,
    val confirmedAt: String,
    val totalAmount: Double,
)

@Serializable
data class SalesOrderDetailsResponse(
    val id: Int,
    val orderNumber: String,
    val customer: TransactionReferenceResponse?,
    val warehouse: TransactionReferenceResponse?,
    val status: SalesOrderStatus,
    val orderedAt: String,
    val confirmedAt: String,
    val notes: String,
    val totalAmount: Double,
    val items: List<SalesOrderItemResponse>,
)
