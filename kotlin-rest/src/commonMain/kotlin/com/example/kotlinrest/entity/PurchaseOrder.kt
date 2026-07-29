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
    @DbField(enumAsString = true)
    var status: PurchaseOrderStatus by db(PurchaseOrderStatus.DRAFT)
    var orderedAt: String? by db(null)
    var expectedAt: String? by db(null)
    var receivedAt: String? by db(null)
    var notes: String by db("")
}
