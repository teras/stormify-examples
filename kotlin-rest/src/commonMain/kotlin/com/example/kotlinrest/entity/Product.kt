package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

@DbTable
class Product(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
    var sku: String by db("")
    var name: String by db("")
    var description: String by db("")
    @DbField(name = "category_id")
    var category: Category? by db(null)
    @DbField(name = "supplier_id")
    var supplier: Supplier? by db(null)
    var unitPrice: Double by db(0.0)
    var reorderLevel: Int by db(0)
    var active: Boolean by db(true)
}
