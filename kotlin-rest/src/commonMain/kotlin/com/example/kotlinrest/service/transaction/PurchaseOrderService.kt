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
import onl.ycode.stormify.TransactionContext
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.details
import onl.ycode.stormify.findById
import onl.ycode.stormify.transaction

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
            val warehouse = ServiceSupport.loadWarehouse(this, request.warehouseId)
            val order = create(
                PurchaseOrder().apply {
                    orderNumber = DocumentNumberGenerator.nextPurchaseOrderNumber()
                    this.supplier = supplier
                    this.warehouse = warehouse
                    status = PurchaseOrderStatus.DRAFT
                    orderedAt = ServiceSupport.now(this@transaction)
                    expectedAt = request.expectedAt.trim()
                    receivedAt = ""
                    notes = request.notes.trim()
                }
            )
            request.items.forEach { input ->
                ServiceSupport.validatePositive(input.quantity, "Purchase order item quantity")
                ServiceSupport.validateNonNegative(input.unitCost, "Purchase order item unit cost")
                create(
                    PurchaseOrderItem().apply {
                        purchaseOrder = order
                        product = ServiceSupport.loadProduct(this@transaction, input.productId)
                        quantity = input.quantity
                        unitCost = input.unitCost
                        lineTotal = input.quantity * input.unitCost
                    }
                )
            }
            toDetailsResponse(this, order)
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
            order.warehouse = ServiceSupport.loadWarehouse(this, request.warehouseId)
            order.expectedAt = request.expectedAt.trim()
            order.notes = request.notes.trim()
            update(order)

            getDetails<PurchaseOrderItem>(order).forEach { delete(it) }
            request.items.forEach { input ->
                ServiceSupport.validatePositive(input.quantity, "Purchase order item quantity")
                ServiceSupport.validateNonNegative(input.unitCost, "Purchase order item unit cost")
                create(
                    PurchaseOrderItem().apply {
                        purchaseOrder = order
                        product = ServiceSupport.loadProduct(this@transaction, input.productId)
                        quantity = input.quantity
                        unitCost = input.unitCost
                        lineTotal = input.quantity * input.unitCost
                    }
                )
            }
            toDetailsResponse(this, order)
        }
    }

    fun receive(id: Int): PurchaseOrderDetailsResponse {
        return transaction {
            val order = loadTx(id)
            if (order.status == PurchaseOrderStatus.RECEIVED) {
                throw ValidationException("Purchase order is already received")
            }
            val warehouse = order.warehouse ?: throw ValidationException("Purchase order warehouse is required")
            getDetails<PurchaseOrderItem>(order).forEach { item ->
                val product = item.product ?: throw ValidationException("Purchase order item product is required")
                val stock = ServiceSupport.loadOrCreateStockItem(this, warehouse, product)
                stock.quantityOnHand += item.quantity
                stock.lastUpdatedAt = ServiceSupport.now(this)
                update(stock)
            }
            order.status = PurchaseOrderStatus.RECEIVED
            order.receivedAt = ServiceSupport.now(this)
            update(order)
            toDetailsResponse(this, order)
        }
    }

    private fun load(id: Int): PurchaseOrder =
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

    private fun TransactionContext.loadTx(id: Int): PurchaseOrder =
        findById<PurchaseOrder>(id) ?: throw EntityNotFoundException("PurchaseOrder", id)

    private fun toDetailsResponse(tx: TransactionContext, order: PurchaseOrder): PurchaseOrderDetailsResponse {
        val items = tx.getDetails<PurchaseOrderItem>(order)
        return PurchaseOrderDetailsResponse(
            id = order.id ?: 0,
            orderNumber = order.orderNumber,
            supplier = ServiceSupport.ref(order.supplier?.id, order.supplier?.name),
            warehouse = ServiceSupport.ref(order.warehouse?.id, order.warehouse?.name),
            status = order.status,
            orderedAt = order.orderedAt,
            expectedAt = order.expectedAt,
            receivedAt = order.receivedAt,
            notes = order.notes,
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
