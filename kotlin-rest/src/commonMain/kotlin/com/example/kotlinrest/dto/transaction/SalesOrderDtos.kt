package com.example.kotlinrest.dto.transaction

import com.example.kotlinrest.dto.common.Ref
import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.dto.common.ref
import com.example.kotlinrest.entity.SalesOrder
import com.example.kotlinrest.entity.SalesOrderItem
import com.example.kotlinrest.entity.SalesOrderStatus
import kotlinx.serialization.Serializable
import onl.ycode.stormify.*

@Serializable
data class SalesOrderItemInput(
    val productId: Int,
    val quantity: Int,
    val unitPrice: Long,
)

@Serializable
data class SalesOrderRequest(
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
    val unitPrice: Long,
    val lineTotal: Long,
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
    val orderedAt: String?,
    val confirmedAt: String?,
    val totalAmount: Long,
)

@Serializable
data class SalesOrderDetailsResponse(
    val id: Int,
    val orderNumber: String,
    val customer: Ref?,
    val warehouse: Ref?,
    val status: SalesOrderStatus,
    val orderedAt: String?,
    val confirmedAt: String?,
    val notes: String,
    val totalAmount: Long,
    val items: List<SalesOrderItemResponse>,
)

fun SalesOrder.toListItemResponse(totalAmount: Long) = SalesOrderListItemResponse(
    id = pk(id),
    orderNumber = orderNumber,
    customerId = customer?.id,
    customerName = customer?.name,
    warehouseId = warehouse?.id,
    warehouseName = warehouse?.name,
    status = status,
    orderedAt = orderedAt,
    confirmedAt = confirmedAt,
    totalAmount = totalAmount,
)

fun SalesOrder.toDetailsResponse(): SalesOrderDetailsResponse {
    val items = details<SalesOrderItem>()
    return SalesOrderDetailsResponse(
        id = pk(id),
        orderNumber = orderNumber,
        customer = ref(customer?.id, customer?.name),
        warehouse = ref(warehouse?.id, warehouse?.name),
        status = status,
        orderedAt = orderedAt,
        confirmedAt = confirmedAt,
        notes = notes,
        totalAmount = items.sumOf { it.lineTotal },
        items = items.map {
            SalesOrderItemResponse(
                id = pk(it.id),
                productId = it.product?.id,
                productSku = it.product?.sku,
                productName = it.product?.name,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                lineTotal = it.lineTotal,
            )
        },
    )
}
