package com.example.kotlinrest.dto.transaction

import com.example.kotlinrest.dto.common.Ref
import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.dto.common.ref
import com.example.kotlinrest.entity.PurchaseOrder
import com.example.kotlinrest.entity.PurchaseOrderItem
import com.example.kotlinrest.entity.PurchaseOrderStatus
import kotlinx.serialization.Serializable
import onl.ycode.stormify.*

@Serializable
data class PurchaseOrderItemInput(
    val productId: Int,
    val quantity: Int,
    val unitCost: Long,
)

@Serializable
data class PurchaseOrderRequest(
    val supplierId: Int,
    val warehouseId: Int,
    val expectedAt: String?,
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
    val unitCost: Long,
    val lineTotal: Long,
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
    val orderedAt: String?,
    val expectedAt: String?,
    val receivedAt: String?,
    val totalAmount: Long,
)

@Serializable
data class PurchaseOrderDetailsResponse(
    val id: Int,
    val orderNumber: String,
    val supplier: Ref?,
    val warehouse: Ref?,
    val status: PurchaseOrderStatus,
    val orderedAt: String?,
    val expectedAt: String?,
    val receivedAt: String?,
    val notes: String,
    val totalAmount: Long,
    val items: List<PurchaseOrderItemResponse>,
)

fun PurchaseOrder.toListItemResponse(totalAmount: Long) = PurchaseOrderListItemResponse(
    id = pk(id),
    orderNumber = orderNumber,
    supplierId = supplier?.id,
    supplierName = supplier?.name,
    warehouseId = warehouse?.id,
    warehouseName = warehouse?.name,
    status = status,
    orderedAt = orderedAt,
    expectedAt = expectedAt,
    receivedAt = receivedAt,
    totalAmount = totalAmount,
)

fun PurchaseOrder.toDetailsResponse(): PurchaseOrderDetailsResponse {
    val items = details<PurchaseOrderItem>()
    return PurchaseOrderDetailsResponse(
        id = pk(id),
        orderNumber = orderNumber,
        supplier = ref(supplier?.id, supplier?.name),
        warehouse = ref(warehouse?.id, warehouse?.name),
        status = status,
        orderedAt = orderedAt,
        expectedAt = expectedAt,
        receivedAt = receivedAt,
        notes = notes,
        totalAmount = items.sumOf { it.lineTotal },
        items = items.map {
            PurchaseOrderItemResponse(
                id = pk(it.id),
                productId = it.product?.id,
                productSku = it.product?.sku,
                productName = it.product?.name,
                quantity = it.quantity,
                unitCost = it.unitCost,
                lineTotal = it.lineTotal,
            )
        },
    )
}
