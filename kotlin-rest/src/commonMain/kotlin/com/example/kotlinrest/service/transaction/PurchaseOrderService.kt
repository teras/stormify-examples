package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.support.now
import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.transaction.PurchaseOrderDetailsResponse
import com.example.kotlinrest.dto.transaction.PurchaseOrderListItemResponse
import com.example.kotlinrest.dto.transaction.PurchaseOrderRequest
import com.example.kotlinrest.dto.transaction.toDetailsResponse
import com.example.kotlinrest.dto.transaction.toListItemResponse
import com.example.kotlinrest.entity.PurchaseOrder
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.entity.PurchaseOrderItem
import com.example.kotlinrest.entity.PurchaseOrderStatus
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ReferenceNotFoundException
import com.example.kotlinrest.service.inventory.Stock
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.PagedQuerySupport
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.support.DocumentNumberGenerator
import com.example.kotlinrest.support.centsToDecimal
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.*
import onl.ycode.stormify.coroutines.SuspendStormify

internal class PurchaseOrderService(private val async: SuspendStormify) {
    private val query = PagedQuery<PurchaseOrder>().apply {
        addFacet("search", "orderNumber", "supplier.name", "warehouse.name").also { it.isSortable = false }
        addFacet("orderNumber", "orderNumber")
        addFacet("supplierName", "supplier.name")
        addFacet("warehouseName", "warehouse.name")
        // `enumAsString` stores the name, so the column is already the text to filter on.
        addSqlFacet("status", "purchase_order.status", Facet.TEXT)
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

    suspend fun search(spec: PageSpec): PagedResponse<PurchaseOrderListItemResponse> = async.withConnection {
        val page = query.execute(PagedQuerySupport.searchSpec(query, spec, defaultSortAlias = "orderNumber"))
        // The totals are not part of the query: fetch them for the whole page in one query
        // rather than once per row.
        val totals = totalsByOrder(page.rows)
        PagedQuerySupport.buildResponse(page) { it.toListItemResponse(totals[it.id] ?: 0L) }
    }

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
            "totalAmount" to { centsToDecimal(it.totalAmount) },
        )
        // The totals are not part of the query, so rows are mapped a chunk at a time, with
        // one totals query per chunk.
        val chunks = CsvSupport.Chunks<PurchaseOrder, PurchaseOrderListItemResponse>(columns, writeLine) { orders ->
            val totals = totalsByOrder(orders)
            orders.map { it.toListItemResponse(totals[it.id] ?: 0L) }
        }
        writeLine(CsvSupport.header(columns))
        // forEachStreaming walks the whole result set, so the pagination fields of the
        // spec are ignored on this path: page 0 with a large page size.
        query.forEachStreaming(spec.copy(page = 0, pageSize = 1000)) { chunks.add(it) }
        chunks.flush()
    }

    suspend fun getById(id: Int): PurchaseOrderDetailsResponse = async.withConnection { load(id).toDetailsResponse() }

    suspend fun create(request: PurchaseOrderRequest): PurchaseOrderDetailsResponse = async.transaction {
        validateItems(request.items.size)
        ServiceSupport.validateOptionalInstant(request.expectedAt, "Expected date", "expectedAt")
        val supplier = findById<Supplier>(request.supplierId)
            ?: throw ReferenceNotFoundException("Supplier", "supplierId", request.supplierId)
        val warehouse = findById<Warehouse>(request.warehouseId)
            ?: throw ReferenceNotFoundException("Warehouse", "warehouseId", request.warehouseId)
        val order = PurchaseOrder().apply {
            orderNumber = DocumentNumberGenerator.nextPurchaseOrderNumber()
            this.supplier = supplier
            this.warehouse = warehouse
            status = PurchaseOrderStatus.DRAFT
            orderedAt = now()
            expectedAt = request.expectedAt?.trim()
            receivedAt = null
            notes = request.notes.trim()
        }.create()
        request.items.forEach { input ->
            ServiceSupport.validatePositive(input.quantity, "Purchase order item quantity")
            ServiceSupport.validateNonNegative(input.unitCost, "Purchase order item unit cost")
            PurchaseOrderItem().apply {
                purchaseOrder = order
                product = findById<Product>(input.productId)
                    ?: throw ReferenceNotFoundException("Product", "productId", input.productId)
                quantity = input.quantity
                unitCost = input.unitCost
                lineTotal = input.quantity * input.unitCost
            }.create()
        }
        order.toDetailsResponse()
    }

    suspend fun update(id: Int, request: PurchaseOrderRequest): PurchaseOrderDetailsResponse = async.transaction {
        validateItems(request.items.size)
        ServiceSupport.validateOptionalInstant(request.expectedAt, "Expected date", "expectedAt")
        val order = load(id)
        ServiceSupport.requireStatus(order.status, PurchaseOrderStatus.DRAFT, "purchase orders", "updated")
        order.supplier = findById<Supplier>(request.supplierId)
            ?: throw ReferenceNotFoundException("Supplier", "supplierId", request.supplierId)
        order.warehouse = findById<Warehouse>(request.warehouseId)
            ?: throw ReferenceNotFoundException("Warehouse", "warehouseId", request.warehouseId)
        order.expectedAt = request.expectedAt?.trim()
        order.notes = request.notes.trim()
        order.update()

        order.details<PurchaseOrderItem>().forEach { it.delete() }
        request.items.forEach { input ->
            ServiceSupport.validatePositive(input.quantity, "Purchase order item quantity")
            ServiceSupport.validateNonNegative(input.unitCost, "Purchase order item unit cost")
            PurchaseOrderItem().apply {
                purchaseOrder = order
                product = findById<Product>(input.productId)
                    ?: throw ReferenceNotFoundException("Product", "productId", input.productId)
                quantity = input.quantity
                unitCost = input.unitCost
                lineTotal = input.quantity * input.unitCost
            }.create()
        }
        order.toDetailsResponse()
    }

    suspend fun receive(id: Int): PurchaseOrderDetailsResponse = async.transaction {
        val order = load(id)
        ServiceSupport.requireStatus(order.status, PurchaseOrderStatus.DRAFT, "purchase orders", "received")
        val warehouse = order.warehouse ?: throw ValidationException("Purchase order warehouse is required")
        order.details<PurchaseOrderItem>().forEach { item ->
            val product = item.product ?: throw ValidationException("Purchase order item product is required")
            Stock.receive(warehouse, product, item.quantity)
        }
        order.status = PurchaseOrderStatus.RECEIVED
        order.receivedAt = now()
        order.update()
        order.toDetailsResponse()
    }

    private fun load(id: Int): PurchaseOrder =
        findById<PurchaseOrder>(id) ?: throw EntityNotFoundException("Purchase order", id)

    private fun validateItems(count: Int) {
        if (count == 0) throw ValidationException("Purchase order must contain at least one item")
    }

    /**
     * One grouped query for the whole page instead of one per row.
     *
     * Passing the order list straight into `IN ?` is a Stormify convenience: a collection
     * of entities expands to its primary keys, so there is no id-extraction step to write
     * and no placeholder counting to get wrong.
     */
    private fun totalsByOrder(orders: List<PurchaseOrder>): Map<Int, Long> {
        if (orders.isEmpty()) return emptyMap()
        return ("SELECT purchase_order_id AS order_id, SUM(line_total) AS total FROM purchase_order_item " +
            "WHERE purchase_order_id IN ? GROUP BY purchase_order_id")
            .read<Map<String, Any>>(orders)
            .associate { row ->
                (row["order_id"] as Number).toInt() to (row["total"] as Number).toLong()
            }
    }
}
