package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.transaction.CreateShipmentRequest
import com.example.kotlinrest.dto.transaction.ShipmentDetailsResponse
import com.example.kotlinrest.dto.transaction.ShipmentListItemResponse
import com.example.kotlinrest.dto.transaction.UpdateShipmentRequest
import com.example.kotlinrest.entity.SalesOrder
import com.example.kotlinrest.entity.SalesOrderItem
import com.example.kotlinrest.entity.SalesOrderStatus
import com.example.kotlinrest.entity.Shipment
import com.example.kotlinrest.entity.ShipmentStatus
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.PagedQuerySupport
import com.example.kotlinrest.service.support.addEnumFacet
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.support.DocumentNumberGenerator
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.*

class ShipmentService {
    private val query = PagedQuery<Shipment>().apply {
        addFacet("search", "shipmentNumber", "salesOrder.orderNumber", "warehouse.name", "carrier", "trackingCode")
            .also { it.isSortable = false }
        addFacet("shipmentNumber", "shipmentNumber")
        addFacet("salesOrderNumber", "salesOrder.orderNumber")
        addFacet("warehouseName", "warehouse.name")
        addFacet("carrier", "carrier")
        addFacet("trackingCode", "trackingCode")
        addEnumFacet<ShipmentStatus>("status", "shipment.status")
        addFacet("salesOrderId", "salesOrder.id")
        addFacet("warehouseId", "warehouse.id")
        addFacet("shippedAt", "shippedAt")
        addFacet("deliveredAt", "deliveredAt")
    }

    fun search(spec: PageSpec): PagedResponse<ShipmentListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "shipmentNumber") { it.toListItemResponse() }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (ShipmentListItemResponse) -> Any?>>(
            "id" to { it.id },
            "shipmentNumber" to { it.shipmentNumber },
            "salesOrderNumber" to { it.salesOrderNumber },
            "warehouseName" to { it.warehouseName },
            "carrier" to { it.carrier },
            "trackingCode" to { it.trackingCode },
            "status" to { it.status },
            "shippedAt" to { it.shippedAt },
            "deliveredAt" to { it.deliveredAt },
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    fun getById(id: Int): ShipmentDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateShipmentRequest): ShipmentDetailsResponse {
        ServiceSupport.validateText(request.carrier, "Carrier")
        val salesOrder = findById<SalesOrder>(request.salesOrderId) ?: throw EntityNotFoundException("SalesOrder", request.salesOrderId)
        val warehouse = ServiceSupport.loadWarehouse(request.warehouseId)
        val shipment = Shipment().apply {
            shipmentNumber = DocumentNumberGenerator.nextShipmentNumber()
            this.salesOrder = salesOrder
            this.warehouse = warehouse
            status = ShipmentStatus.PREPARING
            carrier = request.carrier.trim()
            trackingCode = request.trackingCode.trim()
            shippedAt = ""
            deliveredAt = ""
        }
        shipment.create()
        return shipment.toDetailsResponse()
    }

    fun update(id: Int, request: UpdateShipmentRequest): ShipmentDetailsResponse {
        ServiceSupport.validateText(request.carrier, "Carrier")
        val shipment = load(id)
        if (shipment.status != ShipmentStatus.PREPARING) {
            throw ValidationException("Only preparing shipments can be updated")
        }
        shipment.carrier = request.carrier.trim()
        shipment.trackingCode = request.trackingCode.trim()
        shipment.update()
        return shipment.toDetailsResponse()
    }

    fun ship(id: Int): ShipmentDetailsResponse {
        return transaction {
            val shipment = findById<Shipment>(id) ?: throw EntityNotFoundException("Shipment", id)
            if (shipment.status != ShipmentStatus.PREPARING) {
                throw ValidationException("Only preparing shipments can be shipped")
            }
            val salesOrder = shipment.salesOrder ?: throw ValidationException("Shipment sales order is required")
            val warehouse = shipment.warehouse ?: throw ValidationException("Shipment warehouse is required")
            salesOrder.details<SalesOrderItem>().forEach { item ->
                val product = item.product ?: throw ValidationException("Shipment item product is required")
                val stock = ServiceSupport.loadOrCreateStockItem(warehouse, product)
                if (stock.quantityReserved < item.quantity || stock.quantityOnHand < item.quantity) {
                    throw ValidationException("Shipment cannot be completed because stock is inconsistent for product ${product.sku}")
                }
                stock.quantityReserved -= item.quantity
                stock.quantityOnHand -= item.quantity
                stock.lastUpdatedAt = ServiceSupport.now()
                stock.update()
            }
            shipment.status = ShipmentStatus.SHIPPED
            shipment.shippedAt = ServiceSupport.now()
            shipment.update()
            salesOrder.status = SalesOrderStatus.SHIPPED
            salesOrder.update()
            shipment.toDetailsResponse()
        }
    }

    private fun load(id: Int): Shipment =
        findById<Shipment>(id) ?: throw EntityNotFoundException("Shipment", id)

    private fun Shipment.toListItemResponse() = ShipmentListItemResponse(
        id = id ?: 0,
        shipmentNumber = shipmentNumber,
        salesOrderId = salesOrder?.id,
        salesOrderNumber = salesOrder?.orderNumber,
        warehouseId = warehouse?.id,
        warehouseName = warehouse?.name,
        carrier = carrier,
        trackingCode = trackingCode,
        status = status,
        shippedAt = shippedAt,
        deliveredAt = deliveredAt,
    )

    private fun Shipment.toDetailsResponse() = ShipmentDetailsResponse(
        id = id ?: 0,
        shipmentNumber = shipmentNumber,
        salesOrder = ServiceSupport.ref(salesOrder?.id, salesOrder?.orderNumber),
        warehouse = ServiceSupport.ref(warehouse?.id, warehouse?.name),
        carrier = carrier,
        trackingCode = trackingCode,
        status = status,
        shippedAt = shippedAt,
        deliveredAt = deliveredAt,
    )
}
