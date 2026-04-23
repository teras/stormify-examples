package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.transaction.CreatePurchaseOrderRequest
import com.example.kotlinrest.dto.transaction.PurchaseOrderDetailsResponse
import com.example.kotlinrest.dto.transaction.PurchaseOrderItemResponse
import com.example.kotlinrest.dto.transaction.PurchaseOrderListItemResponse
import com.example.kotlinrest.dto.transaction.UpdatePurchaseOrderRequest
import com.example.kotlinrest.entity.PurchaseOrder
import com.example.kotlinrest.entity.PurchaseOrderItem
import com.example.kotlinrest.entity.PurchaseOrderStatus
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.PagedQuerySupport
import com.example.kotlinrest.service.support.addEnumFacet
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.support.DocumentNumberGenerator
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.*

class PurchaseOrderService {
    private val query = PagedQuery<PurchaseOrder>().apply {
        addFacet("search", "orderNumber", "supplier.name", "warehouse.name").also { it.isSortable = false }
        addFacet("orderNumber", "orderNumber")
        addFacet("supplierName", "supplier.name")
        addFacet("warehouseName", "warehouse.name")
        addEnumFacet<PurchaseOrderStatus>("status", "purchase_order.status")
        addFacet("supplierId", "supplier.id")
        addFacet("warehouseId", "warehouse.id")
        addFacet("orderedAt", "orderedAt")
        addFacet("expectedAt", "expectedAt")
        addFacet("receivedAt", "receivedAt")
        addSqlFacet(
            "totalAmount",
            "(SELECT COALESCE(SUM(line_total), 0) FROM purchase_order_item WHERE purchase_order_id = purchase_order.id)",
            Facet.NUMERIC
        )
    }

    fun search(spec: PageSpec): PagedResponse<PurchaseOrderListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "orderNumber") { it.toListItemResponse() }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (PurchaseOrderListItemResponse) -> Any?>>(
            "id" to { it.id },
            "orderNumber" to { it.orderNumber },
            "supplierName" to { it.supplierName },
            "warehouseName" to { it.warehouseName },
            "status" to { it.status },
            "orderedAt" to { it.orderedAt },
            "expectedAt" to { it.expectedAt },
            "receivedAt" to { it.receivedAt },
            "totalAmount" to { it.totalAmount },
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    fun getById(id: Int): PurchaseOrderDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreatePurchaseOrderRequest): PurchaseOrderDetailsResponse {
        return transaction {
            validateItems(request.items.size)
            val supplier = findById<Supplier>(request.supplierId) ?: throw EntityNotFoundException("Supplier", request.supplierId)
            val warehouse = ServiceSupport.loadWarehouse(request.warehouseId)
            val order = PurchaseOrder().apply {
                orderNumber = DocumentNumberGenerator.nextPurchaseOrderNumber()
                this.supplier = supplier
                this.warehouse = warehouse
                status = PurchaseOrderStatus.DRAFT
                orderedAt = ServiceSupport.now()
                expectedAt = request.expectedAt.trim()
                receivedAt = ""
                notes = request.notes.trim()
            }.create()
            request.items.forEach { input ->
                ServiceSupport.validatePositive(input.quantity, "Purchase order item quantity")
                ServiceSupport.validateNonNegative(input.unitCost, "Purchase order item unit cost")
                PurchaseOrderItem().apply {
                    purchaseOrder = order
                    product = ServiceSupport.loadProduct(input.productId)
                    quantity = input.quantity
                    unitCost = input.unitCost
                    lineTotal = input.quantity * input.unitCost
                }.create()
            }
            order.toDetailsResponse()
        }
    }

    fun update(id: Int, request: UpdatePurchaseOrderRequest): PurchaseOrderDetailsResponse {
        return transaction {
            validateItems(request.items.size)
            val order = loadTx(id)
            if (order.status != PurchaseOrderStatus.DRAFT) {
                throw ValidationException("Only draft purchase orders can be updated")
            }
            order.supplier = findById<Supplier>(request.supplierId) ?: throw EntityNotFoundException("Supplier", request.supplierId)
            order.warehouse = ServiceSupport.loadWarehouse(request.warehouseId)
            order.expectedAt = request.expectedAt.trim()
            order.notes = request.notes.trim()
            order.update()

            order.details<PurchaseOrderItem>().forEach { it.delete() }
            request.items.forEach { input ->
                ServiceSupport.validatePositive(input.quantity, "Purchase order item quantity")
                ServiceSupport.validateNonNegative(input.unitCost, "Purchase order item unit cost")
                PurchaseOrderItem().apply {
                    purchaseOrder = order
                    product = ServiceSupport.loadProduct(input.productId)
                    quantity = input.quantity
                    unitCost = input.unitCost
                    lineTotal = input.quantity * input.unitCost
                }.create()
            }
            order.toDetailsResponse()
        }
    }

    fun receive(id: Int): PurchaseOrderDetailsResponse {
        return transaction {
            val order = loadTx(id)
            if (order.status == PurchaseOrderStatus.RECEIVED) {
                throw ValidationException("Purchase order is already received")
            }
            val warehouse = order.warehouse ?: throw ValidationException("Purchase order warehouse is required")
            order.details<PurchaseOrderItem>().forEach { item ->
                val product = item.product ?: throw ValidationException("Purchase order item product is required")
                val stock = ServiceSupport.loadOrCreateStockItem(warehouse, product)
                stock.quantityOnHand += item.quantity
                stock.lastUpdatedAt = ServiceSupport.now()
                stock.update()
            }
            order.status = PurchaseOrderStatus.RECEIVED
            order.receivedAt = ServiceSupport.now()
            order.update()
            order.toDetailsResponse()
        }
    }

    private fun load(id: Int): PurchaseOrder =
        findById<PurchaseOrder>(id) ?: throw EntityNotFoundException("PurchaseOrder", id)

    private fun loadTx(id: Int): PurchaseOrder =
        findById<PurchaseOrder>(id) ?: throw EntityNotFoundException("PurchaseOrder", id)

    private fun validateItems(count: Int) {
        if (count == 0) throw ValidationException("Purchase order must contain at least one item")
    }

    private fun PurchaseOrder.toListItemResponse(): PurchaseOrderListItemResponse {
        val items = details<PurchaseOrderItem>()
        return PurchaseOrderListItemResponse(
            id = id ?: 0,
            orderNumber = orderNumber,
            supplierId = supplier?.id,
            supplierName = supplier?.name,
            warehouseId = warehouse?.id,
            warehouseName = warehouse?.name,
            status = status,
            orderedAt = orderedAt,
            expectedAt = expectedAt,
            receivedAt = receivedAt,
            totalAmount = items.sumOf { it.lineTotal },
        )
    }

    private fun PurchaseOrder.toDetailsResponse(): PurchaseOrderDetailsResponse {
        val items = details<PurchaseOrderItem>()
        return PurchaseOrderDetailsResponse(
            id = id ?: 0,
            orderNumber = orderNumber,
            supplier = ServiceSupport.ref(supplier?.id, supplier?.name),
            warehouse = ServiceSupport.ref(warehouse?.id, warehouse?.name),
            status = status,
            orderedAt = orderedAt,
            expectedAt = expectedAt,
            receivedAt = receivedAt,
            notes = notes,
            totalAmount = items.sumOf { it.lineTotal },
            items = items.map {
                PurchaseOrderItemResponse(
                    id = it.id ?: 0,
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
}
