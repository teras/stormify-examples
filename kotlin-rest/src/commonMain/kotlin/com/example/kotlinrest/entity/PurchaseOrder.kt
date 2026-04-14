package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

@DbTable
class PurchaseOrder(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
    var orderNumber: String by db("")
    @DbField(name = "supplier_id")
    var supplier: Supplier? by db(null)
    @DbField(name = "warehouse_id")
    var warehouse: Warehouse? by db(null)
    var status: PurchaseOrderStatus by db(PurchaseOrderStatus.DRAFT)
    var orderedAt: String by db("")
    var expectedAt: String by db("")
    var receivedAt: String by db("")
    var notes: String by db("")
}
