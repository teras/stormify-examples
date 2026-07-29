package com.example.kotlinrest.dto.transaction

import com.example.kotlinrest.dto.common.Ref
import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.dto.common.ref
import com.example.kotlinrest.entity.Shipment
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
    val shippedAt: String?,
)

@Serializable
data class ShipmentDetailsResponse(
    val id: Int,
    val shipmentNumber: String,
    val salesOrder: Ref?,
    val warehouse: Ref?,
    val carrier: String,
    val trackingCode: String,
    val status: ShipmentStatus,
    val shippedAt: String?,
)

fun Shipment.toListItemResponse() = ShipmentListItemResponse(
    id = pk(id),
    shipmentNumber = shipmentNumber,
    salesOrderId = salesOrder?.id,
    salesOrderNumber = salesOrder?.orderNumber,
    warehouseId = warehouse?.id,
    warehouseName = warehouse?.name,
    carrier = carrier,
    trackingCode = trackingCode,
    status = status,
    shippedAt = shippedAt,
)

fun Shipment.toDetailsResponse() = ShipmentDetailsResponse(
    id = pk(id),
    shipmentNumber = shipmentNumber,
    salesOrder = ref(salesOrder?.id, salesOrder?.orderNumber),
    warehouse = ref(warehouse?.id, warehouse?.name),
    carrier = carrier,
    trackingCode = trackingCode,
    status = status,
    shippedAt = shippedAt,
)
