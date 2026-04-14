package com.example.kotlinrest.dto.transaction

import com.example.kotlinrest.entity.PurchaseOrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseOrderItemInput(
    val productId: Int,
    val quantity: Int,
    val unitCost: Double,
)

@Serializable
data class CreatePurchaseOrderRequest(
    val supplierId: Int,
    val warehouseId: Int,
    val expectedAt: String,
    val notes: String,
    val items: List<PurchaseOrderItemInput>,
)

@Serializable
data class UpdatePurchaseOrderRequest(
    val supplierId: Int,
    val warehouseId: Int,
    val expectedAt: String,
    val notes: String,
    val items: List<PurchaseOrderItemInput>,
)

@Serializable
data class PurchaseOrderItemResponse(
    val id: Int,
    val productId: Int?,
    val productSku: String?,
    val productName: String?,
    val quantity: Int,
    val unitCost: Double,
    val lineTotal: Double,
)

@Serializable
data class PurchaseOrderListItemResponse(
    val id: Int,
    val orderNumber: String,
    val supplierId: Int?,
    val supplierName: String?,
    val warehouseId: Int?,
    val warehouseName: String?,
    val status: PurchaseOrderStatus,
    val orderedAt: String,
    val expectedAt: String,
    val receivedAt: String,
    val totalAmount: Double,
)

@Serializable
data class PurchaseOrderDetailsResponse(
    val id: Int,
    val orderNumber: String,
    val supplier: TransactionReferenceResponse?,
    val warehouse: TransactionReferenceResponse?,
    val status: PurchaseOrderStatus,
    val orderedAt: String,
    val expectedAt: String,
    val receivedAt: String,
    val notes: String,
    val totalAmount: Double,
    val items: List<PurchaseOrderItemResponse>,
)
