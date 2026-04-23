package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class SalesOrderItem(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
    @DbField(name = "sales_order_id")
    var salesOrder: SalesOrder? by db(null)
    @DbField(name = "product_id")
    var product: Product? by db(null)
    var quantity: Int by db(0)
    var unitPrice: Double by db(0.0)
    var lineTotal: Double by db(0.0)
}
