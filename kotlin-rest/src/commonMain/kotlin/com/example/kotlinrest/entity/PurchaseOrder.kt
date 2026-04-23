package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class PurchaseOrder(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
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
