package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

@DbTable
class StockItem(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
    @DbField(name = "warehouse_id")
    var warehouse: Warehouse? by db(null)
    @DbField(name = "product_id")
    var product: Product? by db(null)
    var quantityOnHand: Int by db(0)
    var quantityReserved: Int by db(0)
    var lastUpdatedAt: String by db("")
}
