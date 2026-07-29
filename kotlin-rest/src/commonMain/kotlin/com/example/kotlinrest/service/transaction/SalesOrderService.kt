package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.support.now
import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.transaction.SalesOrderDetailsResponse
import com.example.kotlinrest.dto.transaction.SalesOrderListItemResponse
import com.example.kotlinrest.dto.transaction.SalesOrderRequest
import com.example.kotlinrest.dto.transaction.toDetailsResponse
import com.example.kotlinrest.dto.transaction.toListItemResponse
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.SalesOrder
import com.example.kotlinrest.entity.SalesOrderItem
import com.example.kotlinrest.entity.SalesOrderStatus
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

internal class SalesOrderService(private val async: SuspendStormify) {
    private val query = PagedQuery<SalesOrder>().apply {
        addFacet("search", "orderNumber", "customer.name", "warehouse.name").also { it.isSortable = false }
        addFacet("orderNumber", "orderNumber")
        addFacet("customerName", "customer.name")
        addFacet("warehouseName", "warehouse.name")
        // `enumAsString` stores the name, so the column is already the text to filter on.
        addSqlFacet("status", "sales_order.status", Facet.TEXT)
        addFacet("customerId", "customer.id")
        addFacet("warehouseId", "warehouse.id")
        addFacet("orderedAt", "orderedAt")
        addFacet("confirmedAt", "confirmedAt")
        addSqlFacet(
            "totalAmount",
            "(SELECT COALESCE(SUM(line_total), 0) FROM sales_order_item WHERE sales_order_id = sales_order.id)",
            Facet.NUMERIC
        )
    }

    suspend fun search(spec: PageSpec): PagedResponse<SalesOrderListItemResponse> = async.withConnection {
        PagedQuerySupport.executePage(query, spec, defaultSortAlias = "orderNumber") { orders ->
            val totals = totalsByOrder(orders)
            orders.map { it.toListItemResponse(totals[it.id] ?: 0L) }
        }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (SalesOrderListItemResponse) -> Any?>>(
            "id" to { it.id },
            "orderNumber" to { it.orderNumber },
            "customerName" to { it.customerName },
            "warehouseName" to { it.warehouseName },
            "status" to { it.status },
            "orderedAt" to { it.orderedAt },
            "confirmedAt" to { it.confirmedAt },
            "totalAmount" to { centsToDecimal(it.totalAmount) },
        )
        CsvSupport.streamChunked(
            query, spec, columns,
            mapper = { orders ->
                val totals = totalsByOrder(orders)
                orders.map { it.toListItemResponse(totals[it.id] ?: 0L) }
            },
            writeLine = writeLine,
        )
    }

    suspend fun getById(id: Int): SalesOrderDetailsResponse = async.withConnection { load(id).toDetailsResponse() }

    suspend fun create(request: SalesOrderRequest): SalesOrderDetailsResponse = async.transaction {
        validateItems(request.items.size)
        val customer = findById<Customer>(request.customerId)
            ?: throw ReferenceNotFoundException("Customer", "customerId", request.customerId)
        val warehouse = ServiceSupport.loadWarehouse(request.warehouseId)
        val order = SalesOrder().apply {
            orderNumber = DocumentNumberGenerator.nextSalesOrderNumber()
            this.customer = customer
            this.warehouse = warehouse
            status = SalesOrderStatus.DRAFT
            orderedAt = now()
            confirmedAt = null
            notes = request.notes.trim()
        }.create()
        request.items.forEach { input ->
            ServiceSupport.validatePositive(input.quantity, "Sales order item quantity")
            ServiceSupport.validateNonNegative(input.unitPrice, "Sales order item unit price")
            SalesOrderItem().apply {
                salesOrder = order
                product = ServiceSupport.loadProduct(input.productId)
                quantity = input.quantity
                unitPrice = input.unitPrice
                lineTotal = input.quantity * input.unitPrice
            }.create()
        }
        order.toDetailsResponse()
    }

    suspend fun update(id: Int, request: SalesOrderRequest): SalesOrderDetailsResponse = async.transaction {
        validateItems(request.items.size)
        val order = load(id)
        ServiceSupport.requireStatus(order.status, SalesOrderStatus.DRAFT, "sales orders", "updated")
        order.customer = findById<Customer>(request.customerId)
            ?: throw ReferenceNotFoundException("Customer", "customerId", request.customerId)
        order.warehouse = ServiceSupport.loadWarehouse(request.warehouseId)
        order.notes = request.notes.trim()
        order.update()
        order.details<SalesOrderItem>().forEach { it.delete() }
        request.items.forEach { input ->
            ServiceSupport.validatePositive(input.quantity, "Sales order item quantity")
            ServiceSupport.validateNonNegative(input.unitPrice, "Sales order item unit price")
            SalesOrderItem().apply {
                salesOrder = order
                product = ServiceSupport.loadProduct(input.productId)
                quantity = input.quantity
                unitPrice = input.unitPrice
                lineTotal = input.quantity * input.unitPrice
            }.create()
        }
        order.toDetailsResponse()
    }

    suspend fun confirm(id: Int): SalesOrderDetailsResponse = async.transaction {
        val order = load(id)
        ServiceSupport.requireStatus(order.status, SalesOrderStatus.DRAFT, "sales orders", "confirmed")
        val warehouse = order.warehouse ?: throw ValidationException("Sales order warehouse is required")
        order.details<SalesOrderItem>().forEach { item ->
            val product = item.product ?: throw ValidationException("Sales order item product is required")
            Stock.reserve(warehouse, product, item.quantity)
        }
        order.status = SalesOrderStatus.CONFIRMED
        order.confirmedAt = now()
        order.update()
        order.toDetailsResponse()
    }

    private fun load(id: Int): SalesOrder =
        findById<SalesOrder>(id) ?: throw EntityNotFoundException("Sales order", id)

    private fun validateItems(count: Int) {
        if (count == 0) throw ValidationException("Sales order must contain at least one item")
    }

    /**
     * One grouped query for the whole page instead of one per row.
     *
     * Passing the order list straight into `IN ?` is a Stormify convenience: a collection
     * of entities expands to its primary keys, so there is no id-extraction step to write
     * and no placeholder counting to get wrong.
     */
    private fun totalsByOrder(orders: List<SalesOrder>): Map<Int, Long> {
        if (orders.isEmpty()) return emptyMap()
        return ("SELECT sales_order_id AS order_id, SUM(line_total) AS total FROM sales_order_item " +
            "WHERE sales_order_id IN ? GROUP BY sales_order_id")
            .read<Map<String, Any>>(orders)
            .associate { row ->
                (row["order_id"] as Number).toInt() to (row["total"] as Number).toLong()
            }
    }
}
