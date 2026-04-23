package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class StockItem(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
    @DbField(name = "warehouse_id")
    var warehouse: Warehouse? by db(null)
    @DbField(name = "product_id")
    var product: Product? by db(null)
    var quantityOnHand: Int by db(0)
    var quantityReserved: Int by db(0)
    var lastUpdatedAt: String by db("")
}
