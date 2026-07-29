@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.kotlinrest.db

import com.example.kotlinrest.entity.Category
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.CustomerType
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.PurchaseOrder
import com.example.kotlinrest.entity.PurchaseOrderItem
import com.example.kotlinrest.entity.PurchaseOrderStatus
import com.example.kotlinrest.entity.SalesOrder
import com.example.kotlinrest.entity.SalesOrderItem
import com.example.kotlinrest.entity.SalesOrderStatus
import com.example.kotlinrest.entity.Shipment
import com.example.kotlinrest.entity.ShipmentStatus
import com.example.kotlinrest.entity.StockItem
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.support.toFixedIsoString
import onl.ycode.stormify.Stormify
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The demo database is created and populated on first run, so a fresh clone needs
 * nothing but the source tree. The schema lives here as plain SQL rather than in a
 * migration tool: one file to read, and the DDL is the same text you would type into
 * `sqlite3`.
 */

private val TABLES = listOf(
    """
    CREATE TABLE category (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        description TEXT NOT NULL,
        active INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE supplier (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        contact_name TEXT NOT NULL,
        email TEXT NOT NULL,
        phone TEXT NOT NULL,
        city TEXT NOT NULL,
        country TEXT NOT NULL,
        active INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE customer (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        email TEXT NOT NULL UNIQUE,
        phone TEXT NOT NULL,
        city TEXT NOT NULL,
        country TEXT NOT NULL,
        customer_type TEXT NOT NULL,
        active INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE warehouse (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT NOT NULL UNIQUE,
        name TEXT NOT NULL,
        city TEXT NOT NULL,
        country TEXT NOT NULL,
        active INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE product (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        sku TEXT NOT NULL UNIQUE,
        name TEXT NOT NULL,
        description TEXT NOT NULL,
        category_id INTEGER REFERENCES category(id),
        supplier_id INTEGER REFERENCES supplier(id),
        unit_price INTEGER NOT NULL,
        reorder_level INTEGER NOT NULL,
        active INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE stock_item (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        warehouse_id INTEGER REFERENCES warehouse(id),
        product_id INTEGER REFERENCES product(id),
        quantity_on_hand INTEGER NOT NULL,
        quantity_reserved INTEGER NOT NULL,
        last_updated_at TEXT,
        UNIQUE (warehouse_id, product_id)
    )
    """,
    """
    CREATE TABLE purchase_order (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_number TEXT NOT NULL UNIQUE,
        supplier_id INTEGER REFERENCES supplier(id),
        warehouse_id INTEGER REFERENCES warehouse(id),
        status TEXT NOT NULL,
        ordered_at TEXT,
        expected_at TEXT,
        received_at TEXT,
        notes TEXT NOT NULL
    )
    """,
    """
    CREATE TABLE purchase_order_item (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        purchase_order_id INTEGER REFERENCES purchase_order(id),
        product_id INTEGER REFERENCES product(id),
        quantity INTEGER NOT NULL,
        unit_cost INTEGER NOT NULL,
        line_total INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE sales_order (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_number TEXT NOT NULL UNIQUE,
        customer_id INTEGER REFERENCES customer(id),
        warehouse_id INTEGER REFERENCES warehouse(id),
        status TEXT NOT NULL,
        ordered_at TEXT,
        confirmed_at TEXT,
        notes TEXT NOT NULL
    )
    """,
    """
    CREATE TABLE sales_order_item (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        sales_order_id INTEGER REFERENCES sales_order(id),
        product_id INTEGER REFERENCES product(id),
        quantity INTEGER NOT NULL,
        unit_price INTEGER NOT NULL,
        line_total INTEGER NOT NULL
    )
    """,
    // Document numbers come from here rather than from the clock, so they are gapless
    // and roll back with the order that consumed them. See DocumentNumberGenerator.
    """
    CREATE TABLE doc_counter (
        prefix TEXT PRIMARY KEY,
        next_value INTEGER NOT NULL
    )
    """,
    // One shipment per sales order: the reservation that `ship` consumes belongs to
    // the order, so a second shipment would deduct the same stock twice.
    """
    CREATE TABLE shipment (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        shipment_number TEXT NOT NULL UNIQUE,
        sales_order_id INTEGER UNIQUE REFERENCES sales_order(id),
        warehouse_id INTEGER REFERENCES warehouse(id),
        status TEXT NOT NULL,
        carrier TEXT NOT NULL,
        tracking_code TEXT NOT NULL,
        shipped_at TEXT
    )
    """,
)

// Every foreign key the paged searches join or filter on. Without these each page
// of orders full-scans its item table to compute totals.
private val INDEXES = listOf(
    "CREATE INDEX idx_product_category ON product(category_id)",
    "CREATE INDEX idx_product_supplier ON product(supplier_id)",
    "CREATE INDEX idx_stock_item_warehouse ON stock_item(warehouse_id)",
    "CREATE INDEX idx_stock_item_product ON stock_item(product_id)",
    "CREATE INDEX idx_purchase_order_supplier ON purchase_order(supplier_id)",
    "CREATE INDEX idx_purchase_order_warehouse ON purchase_order(warehouse_id)",
    "CREATE INDEX idx_purchase_order_item_order ON purchase_order_item(purchase_order_id)",
    "CREATE INDEX idx_purchase_order_item_product ON purchase_order_item(product_id)",
    "CREATE INDEX idx_sales_order_customer ON sales_order(customer_id)",
    "CREATE INDEX idx_sales_order_warehouse ON sales_order(warehouse_id)",
    "CREATE INDEX idx_sales_order_item_order ON sales_order_item(sales_order_id)",
    "CREATE INDEX idx_sales_order_item_product ON sales_order_item(product_id)",
    "CREATE INDEX idx_shipment_warehouse ON shipment(warehouse_id)",
)

/**
 * Creates and seeds the schema unless it is already there.
 *
 * The presence of the `category` table is the marker: the schema and its data are
 * written in one transaction, so either both are present or neither is, and a run
 * interrupted halfway leaves nothing to reconcile.
 */
internal fun applySchemaIfNeeded(stormify: Stormify) {
    // Durability settings belong to the file rather than to a session, and SQLite
    // refuses a journal-mode change inside a transaction — so they run first, alone.
    stormify.executeUpdate("PRAGMA journal_mode = WAL")
    stormify.executeUpdate("PRAGMA synchronous = NORMAL")

    val exists = stormify.readOne<Int>(
        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'category'"
    ) ?: 0
    if (exists > 0) return

    // Everything inside a transaction block reaches the same connection, so the tables
    // and the rows that fill them either both land or neither does.
    stormify.transaction {
        for (ddl in TABLES) stormify.executeUpdate(ddl.trimIndent())
        for (ddl in INDEXES) stormify.executeUpdate(ddl)
        seed(stormify)
    }
}

// A fixed seed and a fixed clock: the same clone produces the same database, so a
// screenshot in the README still matches what you see, and a failing query can be
// reproduced from the source alone.
private const val SEED = 42
private val EPOCH = Instant.fromEpochSeconds(1_735_689_600L) // 2025-01-01T00:00:00Z

private fun at(daysFromEpoch: Int, hour: Int): String =
    (EPOCH + (daysFromEpoch * 24 + hour).hours).toFixedIsoString()

private val CITIES = listOf(
    "Athens", "Thessaloniki", "Patras", "Heraklion", "Larissa",
    "Volos", "Ioannina", "Chania", "Kavala", "Rhodes",
)
private val COUNTRIES = listOf("GR", "DE", "FR", "IT", "ES", "NL")
private val CARRIERS = listOf("Speedex", "ACS", "Geniki", "DHL", "UPS")

/**
 * Writes the demo dataset. Volumes match what the example is documented to show:
 * enough rows for paging, sorting and faceted search to behave like a real dataset,
 * few enough to seed in well under a second.
 */
private fun seed(stormify: Stormify) {
    val rnd = Random(SEED)

    val categories = (1..100).map { i ->
        stormify.create(Category().apply {
            name = "Category ${i.toString().padStart(3, '0')}"
            description = "Demo category $i"
            active = i % 20 != 0
        })
    }

    val suppliers = (1..22).map { i ->
        stormify.create(Supplier().apply {
            name = "Supplier ${i.toString().padStart(2, '0')}"
            contactName = "Contact $i"
            email = "supplier$i@example.com"
            phone = "+30 210 000${i.toString().padStart(4, '0')}"
            city = CITIES[rnd.nextInt(CITIES.size)]
            country = COUNTRIES[rnd.nextInt(COUNTRIES.size)]
            active = i % 11 != 0
        })
    }

    val warehouses = (1..10).map { i ->
        stormify.create(Warehouse().apply {
            code = "WH-${i.toString().padStart(2, '0')}"
            name = "${CITIES[i - 1]} Warehouse"
            city = CITIES[i - 1]
            country = "GR"
            active = i != 10
        })
    }

    val customers = (1..50).map { i ->
        stormify.create(Customer().apply {
            name = "Customer ${i.toString().padStart(2, '0')}"
            email = "customer$i@example.com"
            phone = "+30 211 000${i.toString().padStart(4, '0')}"
            city = CITIES[rnd.nextInt(CITIES.size)]
            country = COUNTRIES[rnd.nextInt(COUNTRIES.size)]
            customerType = if (i % 4 == 0) CustomerType.WHOLESALE else CustomerType.RETAIL
            active = i % 13 != 0
        })
    }

    val products = (1..300).map { i ->
        stormify.create(Product().apply {
            sku = "SKU-${i.toString().padStart(4, '0')}"
            name = "Product ${i.toString().padStart(3, '0')}"
            description = "Demo product $i"
            category = categories[rnd.nextInt(categories.size)]
            supplier = suppliers[rnd.nextInt(suppliers.size)]
            unitPrice = (rnd.nextInt(199, 100_000)).toLong()
            reorderLevel = rnd.nextInt(0, 50)
            active = i % 17 != 0
        })
    }

    // 30 distinct (warehouse, product) pairs — the UNIQUE constraint would reject a repeat.
    val stockPairs = mutableSetOf<Pair<Int, Int>>()
    while (stockPairs.size < 30)
        stockPairs.add(rnd.nextInt(warehouses.size) to rnd.nextInt(products.size))
    stormify.create(stockPairs.mapIndexed { i, (w, p) ->
        val onHand = rnd.nextInt(0, 500)
        StockItem().apply {
            warehouse = warehouses[w]
            product = products[p]
            quantityOnHand = onHand
            quantityReserved = if (onHand == 0) 0 else rnd.nextInt(0, onHand / 2 + 1)
            lastUpdatedAt = at(i, 9)
        }
    })

    val purchaseItems = mutableListOf<PurchaseOrderItem>()
    for (i in 1..50) {
        val received = i % 3 == 0
        val orderedDay = i * 2
        val order = stormify.create(PurchaseOrder().apply {
            orderNumber = "PO-${i.toString().padStart(6, '0')}"
            supplier = suppliers[rnd.nextInt(suppliers.size)]
            warehouse = warehouses[rnd.nextInt(warehouses.size)]
            status = if (received) PurchaseOrderStatus.RECEIVED else PurchaseOrderStatus.DRAFT
            orderedAt = at(orderedDay, 10)
            expectedAt = at(orderedDay + 14, 10)
            receivedAt = if (received) at(orderedDay + 12, 15) else null
            notes = if (i % 5 == 0) "Priority restock" else ""
        })
        repeat(rnd.nextInt(3, 8)) {
            val product = products[rnd.nextInt(products.size)]
            val qty = rnd.nextInt(1, 40)
            val cost = product.unitPrice * 7 / 10
            purchaseItems += PurchaseOrderItem().apply {
                purchaseOrder = order
                this.product = product
                quantity = qty
                unitCost = cost
                lineTotal = qty * cost
            }
        }
    }
    stormify.create(purchaseItems)

    val salesItems = mutableListOf<SalesOrderItem>()
    val nonDraftOrders = mutableListOf<SalesOrder>()
    for (i in 1..150) {
        val status = when (i % 5) {
            0, 1 -> SalesOrderStatus.DRAFT
            2, 3 -> SalesOrderStatus.CONFIRMED
            else -> SalesOrderStatus.SHIPPED
        }
        val orderedDay = i
        val order = stormify.create(SalesOrder().apply {
            orderNumber = "SO-${i.toString().padStart(6, '0')}"
            customer = customers[rnd.nextInt(customers.size)]
            warehouse = warehouses[rnd.nextInt(warehouses.size)]
            this.status = status
            orderedAt = at(orderedDay, 11)
            confirmedAt = if (status == SalesOrderStatus.DRAFT) null else at(orderedDay + 1, 9)
            notes = if (i % 7 == 0) "Gift wrap" else ""
        })
        if (status != SalesOrderStatus.DRAFT) nonDraftOrders += order
        repeat(rnd.nextInt(3, 8)) {
            val product = products[rnd.nextInt(products.size)]
            val qty = rnd.nextInt(1, 12)
            salesItems += SalesOrderItem().apply {
                salesOrder = order
                this.product = product
                quantity = qty
                unitPrice = product.unitPrice
                lineTotal = qty * product.unitPrice
            }
        }
    }
    stormify.create(salesItems)

    // A shipment ships from the warehouse the order reserved against, so it always
    // carries the order's own warehouse rather than a random one.
    stormify.create(nonDraftOrders.take(42).mapIndexed { i, order ->
        val shipped = order.status == SalesOrderStatus.SHIPPED
        Shipment().apply {
            shipmentNumber = "SHP-${(i + 1).toString().padStart(6, '0')}"
            salesOrder = order
            warehouse = order.warehouse
            status = if (shipped) ShipmentStatus.SHIPPED else ShipmentStatus.PREPARING
            carrier = CARRIERS[i % CARRIERS.size]
            trackingCode = "TRK${(100000 + i * 37)}"
            shippedAt = if (shipped) at(i + 3, 16) else null
        }
    })

    // Leave the counters where the seed stopped, so the first document created through
    // the API continues the sequence instead of colliding with a seeded number.
    for ((prefix, used) in listOf("PO" to 50, "SO" to 150, "SHP" to 42))
        stormify.executeUpdate(
            "INSERT INTO doc_counter (prefix, next_value) VALUES (?, ?)", prefix, used + 1
        )
}
