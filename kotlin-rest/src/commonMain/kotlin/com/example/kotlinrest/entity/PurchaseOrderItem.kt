package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class PurchaseOrderItem(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
    @DbField(name = "purchase_order_id")
    var purchaseOrder: PurchaseOrder? by db(null)
    @DbField(name = "product_id")
    var product: Product? by db(null)
    var quantity: Int by db(0)
    var unitCost: Double by db(0.0)
    var lineTotal: Double by db(0.0)
}
