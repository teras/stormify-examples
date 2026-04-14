package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

@DbTable
class SalesOrder(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
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
