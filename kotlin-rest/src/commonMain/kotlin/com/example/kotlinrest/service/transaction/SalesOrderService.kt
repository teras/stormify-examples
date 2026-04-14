package com.example.kotlinrest.service.transaction

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.transaction.CreateSalesOrderRequest
import com.example.kotlinrest.dto.transaction.SalesOrderDetailsResponse
import com.example.kotlinrest.dto.transaction.SalesOrderItemResponse
import com.example.kotlinrest.dto.transaction.SalesOrderListItemResponse
import com.example.kotlinrest.dto.transaction.UpdateSalesOrderRequest
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.SalesOrder
import com.example.kotlinrest.entity.SalesOrderItem
import com.example.kotlinrest.entity.SalesOrderStatus
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

class SalesOrderService {
    private val query = PagedQuery<SalesOrder>().apply {
        addFacet("search", "orderNumber", "customer.name", "warehouse.name").also { it.isSortable = false }
        addFacet("orderNumber", "orderNumber")
        addFacet("customerName", "customer.name")
        addFacet("warehouseName", "warehouse.name")
        addEnumFacet<SalesOrderStatus>("status", "sales_order.status")
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

    fun search(spec: PageSpec): PagedResponse<SalesOrderListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "orderNumber") { it.toListItemResponse() }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (SalesOrderListItemResponse) -> Any?>>(
            "id" to { it.id },
            "orderNumber" to { it.orderNumber },
            "customerName" to { it.customerName },
            "warehouseName" to { it.warehouseName },
            "status" to { it.status },
            "orderedAt" to { it.orderedAt },
            "confirmedAt" to { it.confirmedAt },
            "totalAmount" to { it.totalAmount },
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    fun getById(id: Int): SalesOrderDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateSalesOrderRequest): SalesOrderDetailsResponse {
        return transaction {
            validateItems(request.items.size)
            val customer = findById<Customer>(request.customerId) ?: throw EntityNotFoundException("Customer", request.customerId)
            val warehouse = ServiceSupport.loadWarehouse(this, request.warehouseId)
            val order = create(
                SalesOrder().apply {
                    orderNumber = DocumentNumberGenerator.nextSalesOrderNumber()
                    this.customer = customer
                    this.warehouse = warehouse
                    status = SalesOrderStatus.DRAFT
                    orderedAt = ServiceSupport.now(this@transaction)
                    confirmedAt = ""
                    notes = request.notes.trim()
                }
            )
            request.items.forEach { input ->
                ServiceSupport.validatePositive(input.quantity, "Sales order item quantity")
                ServiceSupport.validateNonNegative(input.unitPrice, "Sales order item unit price")
                create(
                    SalesOrderItem().apply {
                        salesOrder = order
                        product = ServiceSupport.loadProduct(this@transaction, input.productId)
                        quantity = input.quantity
                        unitPrice = input.unitPrice
                        lineTotal = input.quantity * input.unitPrice
                    }
                )
            }
            toDetailsResponse(this, order)
        }
    }

    fun update(id: Int, request: UpdateSalesOrderRequest): SalesOrderDetailsResponse {
        return transaction {
            validateItems(request.items.size)
            val order = loadTx(id)
            if (order.status != SalesOrderStatus.DRAFT) {
                throw ValidationException("Only draft sales orders can be updated")
            }
            order.customer = findById<Customer>(request.customerId) ?: throw EntityNotFoundException("Customer", request.customerId)
            order.warehouse = ServiceSupport.loadWarehouse(this, request.warehouseId)
            order.notes = request.notes.trim()
            update(order)
            getDetails<SalesOrderItem>(order).forEach { delete(it) }
            request.items.forEach { input ->
                ServiceSupport.validatePositive(input.quantity, "Sales order item quantity")
                ServiceSupport.validateNonNegative(input.unitPrice, "Sales order item unit price")
                create(
                    SalesOrderItem().apply {
                        salesOrder = order
                        product = ServiceSupport.loadProduct(this@transaction, input.productId)
                        quantity = input.quantity
                        unitPrice = input.unitPrice
                        lineTotal = input.quantity * input.unitPrice
                    }
                )
            }
            toDetailsResponse(this, order)
        }
    }

    fun confirm(id: Int): SalesOrderDetailsResponse {
        return transaction {
            val order = loadTx(id)
            if (order.status != SalesOrderStatus.DRAFT) {
                throw ValidationException("Only draft sales orders can be confirmed")
            }
            val warehouse = order.warehouse ?: throw ValidationException("Sales order warehouse is required")
            getDetails<SalesOrderItem>(order).forEach { item ->
                val product = item.product ?: throw ValidationException("Sales order item product is required")
                val stock = ServiceSupport.loadOrCreateStockItem(this, warehouse, product)
                val available = stock.quantityOnHand - stock.quantityReserved
                if (available < item.quantity) {
                    throw ValidationException("Insufficient available stock for product ${product.sku}")
                }
                stock.quantityReserved += item.quantity
                stock.lastUpdatedAt = ServiceSupport.now(this)
                update(stock)
            }
            order.status = SalesOrderStatus.CONFIRMED
            order.confirmedAt = ServiceSupport.now(this)
            update(order)
            toDetailsResponse(this, order)
        }
    }

    private fun load(id: Int): SalesOrder =
        findById<SalesOrder>(id) ?: throw EntityNotFoundException("SalesOrder", id)

    private fun validateItems(count: Int) {
        if (count == 0) throw ValidationException("Sales order must contain at least one item")
    }

    private fun SalesOrder.toListItemResponse(): SalesOrderListItemResponse {
        val items = details<SalesOrderItem>()
        return SalesOrderListItemResponse(
            id = id ?: 0,
            orderNumber = orderNumber,
            customerId = customer?.id,
            customerName = customer?.name,
            warehouseId = warehouse?.id,
            warehouseName = warehouse?.name,
            status = status,
            orderedAt = orderedAt,
            confirmedAt = confirmedAt,
            totalAmount = items.sumOf { it.lineTotal },
        )
    }

    private fun SalesOrder.toDetailsResponse(): SalesOrderDetailsResponse {
        val items = details<SalesOrderItem>()
        return SalesOrderDetailsResponse(
            id = id ?: 0,
            orderNumber = orderNumber,
            customer = ServiceSupport.ref(customer?.id, customer?.name),
            warehouse = ServiceSupport.ref(warehouse?.id, warehouse?.name),
            status = status,
            orderedAt = orderedAt,
            confirmedAt = confirmedAt,
            notes = notes,
            totalAmount = items.sumOf { it.lineTotal },
            items = items.map {
                SalesOrderItemResponse(
                    id = it.id ?: 0,
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

    private fun TransactionContext.loadTx(id: Int): SalesOrder =
        findById<SalesOrder>(id) ?: throw EntityNotFoundException("SalesOrder", id)

    private fun toDetailsResponse(tx: TransactionContext, order: SalesOrder): SalesOrderDetailsResponse {
        val items = tx.getDetails<SalesOrderItem>(order)
        return SalesOrderDetailsResponse(
            id = order.id ?: 0,
            orderNumber = order.orderNumber,
            customer = ServiceSupport.ref(order.customer?.id, order.customer?.name),
            warehouse = ServiceSupport.ref(order.warehouse?.id, order.warehouse?.name),
            status = order.status,
            orderedAt = order.orderedAt,
            confirmedAt = order.confirmedAt,
            notes = order.notes,
            totalAmount = items.sumOf { it.lineTotal },
            items = items.map {
                SalesOrderItemResponse(
                    id = it.id ?: 0,
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
}
