package com.example.kotlinrest.test

import com.example.kotlinrest.dto.masterdata.CustomerRequest
import com.example.kotlinrest.dto.masterdata.ProductRequest
import com.example.kotlinrest.dto.masterdata.SupplierRequest
import com.example.kotlinrest.dto.masterdata.WarehouseRequest
import com.example.kotlinrest.dto.transaction.CreateShipmentRequest
import com.example.kotlinrest.dto.transaction.PurchaseOrderItemInput
import com.example.kotlinrest.dto.transaction.PurchaseOrderRequest
import com.example.kotlinrest.dto.transaction.SalesOrderItemInput
import com.example.kotlinrest.dto.transaction.SalesOrderRequest
import com.example.kotlinrest.entity.CustomerType
import com.example.kotlinrest.exception.ConflictException
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.masterdata.CategoryService
import com.example.kotlinrest.service.masterdata.CustomerService
import com.example.kotlinrest.service.masterdata.ProductService
import com.example.kotlinrest.service.masterdata.SupplierService
import com.example.kotlinrest.service.masterdata.WarehouseService
import com.example.kotlinrest.service.transaction.PurchaseOrderService
import com.example.kotlinrest.service.transaction.SalesOrderService
import com.example.kotlinrest.service.transaction.ShipmentService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Service-level tests against a temp SQLite file — no HTTP layer, which keeps the suite simple
 * on Kotlin/Native. Each test drives the same suspend services the routes call, so the
 * business rules are exercised exactly as the server runs them.
 */
class ServiceTest {

    /**
     * A conflict raised inside `async.transaction { }` reaches the caller wrapped in the
     * transaction's `SQLException` (the same value the HTTP layer unwraps before answering
     * 409). Outside a transaction it arrives directly. This accepts either shape.
     */
    private inline fun assertConflict(block: () -> Unit) {
        val error = assertFailsWith<Throwable> { block() }
        val conflict = error as? ConflictException ?: error.cause as? ConflictException
        assertNotNull(conflict, "expected a ConflictException, got: $error")
    }

    @Test
    fun purchaseOrderNumbersAreSequential() = runTest {
        val db = freshDatabase("po-sequence")
        val async = db.suspending
        val suppliers = SupplierService(async)
        val warehouses = WarehouseService(async)
        val products = ProductService(async)
        val purchaseOrders = PurchaseOrderService(async)

        val sup = suppliers.create(SupplierRequest("Acme", "Jo", "a@a", "1", "City", "Country", true))
        val wh = warehouses.create(WarehouseRequest("W1", "Main", "City", "Country", true))
        val prod = products.create(ProductRequest("SKU1", "Widget", "", null, null, 100L, 0, true))
        val item = PurchaseOrderItemInput(prod.id, 1, 50L)

        val first = purchaseOrders.create(PurchaseOrderRequest(sup.id, wh.id, null, "", listOf(item)))
        val second = purchaseOrders.create(PurchaseOrderRequest(sup.id, wh.id, null, "", listOf(item)))

        val firstNo = first.orderNumber.substringAfterLast('-').toInt()
        val secondNo = second.orderNumber.substringAfterLast('-').toInt()
        assertEquals(firstNo + 1, secondNo, "order numbers must increase by exactly one")

        async.close()
    }

    @Test
    fun negativeReorderLevelIsRejected() = runTest {
        val db = freshDatabase("validation")
        val async = db.suspending
        val products = ProductService(async)
        assertFailsWith<ValidationException> {
            products.create(ProductRequest("SKU-X", "Bad", "", null, null, 100L, -1, true))
        }
        async.close()
    }

    @Test
    fun deletingMissingCategoryIsNotFound() = runTest {
        val db = freshDatabase("delete-missing")
        val async = db.suspending
        val categories = CategoryService(async)
        assertFailsWith<EntityNotFoundException> {
            categories.delete(999_999)
        }
        async.close()
    }

    @Test
    fun confirmingBeyondStockConflicts() = runTest {
        val db = freshDatabase("insufficient-stock")
        val async = db.suspending
        val warehouses = WarehouseService(async)
        val customers = CustomerService(async)
        val products = ProductService(async)
        val salesOrders = SalesOrderService(async)

        val wh = warehouses.create(WarehouseRequest("W1", "Main", "City", "Country", true))
        val cust = customers.create(CustomerRequest("Bob", "b@b", "1", "City", "Country", CustomerType.RETAIL, true))
        val prod = products.create(ProductRequest("SKU1", "Widget", "", null, null, 100L, 0, true))

        // No stock was ever received, so reserving anything at confirm time must fail.
        val so = salesOrders.create(
            SalesOrderRequest(cust.id, wh.id, "", listOf(SalesOrderItemInput(prod.id, 5, 100L)))
        )
        assertConflict { salesOrders.confirm(so.id) }
        async.close()
    }

    @Test
    fun oneShipmentPerOrder() = runTest {
        val db = freshDatabase("one-shipment")
        val async = db.suspending
        val suppliers = SupplierService(async)
        val warehouses = WarehouseService(async)
        val customers = CustomerService(async)
        val products = ProductService(async)
        val purchaseOrders = PurchaseOrderService(async)
        val salesOrders = SalesOrderService(async)
        val shipments = ShipmentService(async)

        val sup = suppliers.create(SupplierRequest("Acme", "Jo", "a@a", "1", "City", "Country", true))
        val wh = warehouses.create(WarehouseRequest("W1", "Main", "City", "Country", true))
        val cust = customers.create(CustomerRequest("Bob", "b@b", "1", "City", "Country", CustomerType.RETAIL, true))
        val prod = products.create(ProductRequest("SKU1", "Widget", "", null, null, 100L, 0, true))

        // Receive stock so the order can be confirmed (reserve) and shipped.
        val po = purchaseOrders.create(
            PurchaseOrderRequest(sup.id, wh.id, null, "", listOf(PurchaseOrderItemInput(prod.id, 10, 50L)))
        )
        purchaseOrders.receive(po.id)

        val so = salesOrders.create(
            SalesOrderRequest(cust.id, wh.id, "", listOf(SalesOrderItemInput(prod.id, 2, 100L)))
        )
        salesOrders.confirm(so.id)

        shipments.create(CreateShipmentRequest(so.id, wh.id, "UPS", "T1"))
        // The order already has a shipment; a second one is refused.
        assertConflict { shipments.create(CreateShipmentRequest(so.id, wh.id, "UPS", "T2")) }
        async.close()
    }
}
