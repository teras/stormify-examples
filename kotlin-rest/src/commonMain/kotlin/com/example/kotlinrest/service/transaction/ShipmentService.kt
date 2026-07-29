package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.support.now
import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.transaction.CreateShipmentRequest
import com.example.kotlinrest.dto.transaction.ShipmentDetailsResponse
import com.example.kotlinrest.dto.transaction.ShipmentListItemResponse
import com.example.kotlinrest.dto.transaction.UpdateShipmentRequest
import com.example.kotlinrest.dto.transaction.toDetailsResponse
import com.example.kotlinrest.dto.transaction.toListItemResponse
import com.example.kotlinrest.entity.SalesOrder
import com.example.kotlinrest.entity.SalesOrderItem
import com.example.kotlinrest.entity.SalesOrderStatus
import com.example.kotlinrest.entity.Shipment
import com.example.kotlinrest.entity.ShipmentStatus
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ReferenceNotFoundException
import com.example.kotlinrest.exception.ConflictException
import com.example.kotlinrest.service.inventory.Stock
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.PagedQuerySupport
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.support.DocumentNumberGenerator
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.*
import onl.ycode.stormify.coroutines.SuspendStormify

internal class ShipmentService(private val async: SuspendStormify) {
    private val query = PagedQuery<Shipment>().apply {
        addFacet("search", "shipmentNumber", "salesOrder.orderNumber", "warehouse.name", "carrier", "trackingCode")
            .also { it.isSortable = false }
        addFacet("shipmentNumber", "shipmentNumber")
        addFacet("salesOrderNumber", "salesOrder.orderNumber")
        addFacet("warehouseName", "warehouse.name")
        addFacet("carrier", "carrier")
        addFacet("trackingCode", "trackingCode")
        // `enumAsString` stores the name, so the column is already the text to filter on.
        addSqlFacet("status", "shipment.status", Facet.TEXT)
        addFacet("salesOrderId", "salesOrder.id")
        addFacet("warehouseId", "warehouse.id")
        addFacet("shippedAt", "shippedAt")
    }

    suspend fun search(spec: PageSpec): PagedResponse<ShipmentListItemResponse> = async.withConnection {
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "shipmentNumber") { it.toListItemResponse() }
    }

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
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    suspend fun getById(id: Int): ShipmentDetailsResponse = async.withConnection { load(id).toDetailsResponse() }

    suspend fun create(request: CreateShipmentRequest): ShipmentDetailsResponse = async.withConnection {
        // Carrier is required, trackingCode deliberately is not: a shipment is created in
        // PREPARING before the carrier hands back a tracking number, so it is filled in later.
        ServiceSupport.validateText(request.carrier, "Carrier")
        val salesOrder = findById<SalesOrder>(request.salesOrderId)
            ?: throw ReferenceNotFoundException("Sales order", "salesOrderId", request.salesOrderId)
        // Stock is reserved by `confirm`, so there is nothing to ship before it runs.
        if (salesOrder.status != SalesOrderStatus.CONFIRMED)
            throw ConflictException("Only confirmed sales orders can be shipped")
        val warehouse = ServiceSupport.loadWarehouse(request.warehouseId)
        // The reservation was made against the order's warehouse. Shipping from a
        // different one would deduct stock that was never promised here, and leave the
        // promised stock reserved forever.
        if (warehouse.id != salesOrder.warehouse?.id)
            throw ConflictException("A shipment must leave from the warehouse the order reserved against")
        // `UNIQUE (sales_order_id)` in the schema is the real guarantee; this turns the
        // constraint violation into a message that says what went wrong.
        if (findAll<Shipment>("WHERE sales_order_id = ?", salesOrder.id).isNotEmpty())
            throw ConflictException("This sales order already has a shipment")
        val shipment = Shipment().apply {
            shipmentNumber = DocumentNumberGenerator.nextShipmentNumber()
            this.salesOrder = salesOrder
            this.warehouse = warehouse
            status = ShipmentStatus.PREPARING
            carrier = request.carrier.trim()
            trackingCode = request.trackingCode.trim()
            shippedAt = null
        }
        shipment.create()
        shipment.toDetailsResponse()
    }

    suspend fun update(id: Int, request: UpdateShipmentRequest): ShipmentDetailsResponse = async.withConnection {
        ServiceSupport.validateText(request.carrier, "Carrier")
        val shipment = load(id)
        ServiceSupport.requireStatus(shipment.status, ShipmentStatus.PREPARING, "shipments", "updated")
        shipment.carrier = request.carrier.trim()
        shipment.trackingCode = request.trackingCode.trim()
        shipment.update()
        shipment.toDetailsResponse()
    }

    suspend fun ship(id: Int): ShipmentDetailsResponse = async.transaction {
        val shipment = findById<Shipment>(id) ?: throw EntityNotFoundException("Shipment", id)
        ServiceSupport.requireStatus(shipment.status, ShipmentStatus.PREPARING, "shipments", "shipped")
        val salesOrder = shipment.salesOrder ?: throw ValidationException("Shipment sales order is required")
        val warehouse = shipment.warehouse ?: throw ValidationException("Shipment warehouse is required")
        salesOrder.details<SalesOrderItem>().forEach { item ->
            val product = item.product ?: throw ValidationException("Shipment item product is required")
            Stock.deduct(warehouse, product, item.quantity)
        }
        shipment.status = ShipmentStatus.SHIPPED
        shipment.shippedAt = now()
        shipment.update()
        salesOrder.status = SalesOrderStatus.SHIPPED
        salesOrder.update()
        shipment.toDetailsResponse()
    }

    private fun load(id: Int): Shipment =
        findById<Shipment>(id) ?: throw EntityNotFoundException("Shipment", id)
}
