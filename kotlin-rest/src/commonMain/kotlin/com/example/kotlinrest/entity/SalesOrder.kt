package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class SalesOrder(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
    var orderNumber: String by db("")
    @DbField(name = "customer_id")
    var customer: Customer? by db(null)
    @DbField(name = "warehouse_id")
    var warehouse: Warehouse? by db(null)
    var status: SalesOrderStatus by db(SalesOrderStatus.DRAFT)
    var orderedAt: String by db("")
    var confirmedAt: String by db("")
    var notes: String by db("")
}
