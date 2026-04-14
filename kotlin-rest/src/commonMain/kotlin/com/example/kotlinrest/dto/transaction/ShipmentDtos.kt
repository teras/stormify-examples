package com.example.kotlinrest.dto.transaction

import com.example.kotlinrest.entity.ShipmentStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreateShipmentRequest(
    val salesOrderId: Int,
    val warehouseId: Int,
    val carrier: String,
    val trackingCode: String,
)

@Serializable
data class UpdateShipmentRequest(
    val carrier: String,
    val trackingCode: String,
)

@Serializable
data class TransactionReferenceResponse(
    val id: Int,
    val label: String,
)

@Serializable
data class ShipmentListItemResponse(
    val id: Int,
    val shipmentNumber: String,
    val salesOrderId: Int?,
    val salesOrderNumber: String?,
    val warehouseId: Int?,
    val warehouseName: String?,
    val carrier: String,
    val trackingCode: String,
    val status: ShipmentStatus,
    val shippedAt: String,
    val deliveredAt: String,
)

@Serializable
data class ShipmentDetailsResponse(
    val id: Int,
    val shipmentNumber: String,
    val salesOrder: TransactionReferenceResponse?,
    val warehouse: TransactionReferenceResponse?,
    val carrier: String,
    val trackingCode: String,
    val status: ShipmentStatus,
    val shippedAt: String,
    val deliveredAt: String,
)
