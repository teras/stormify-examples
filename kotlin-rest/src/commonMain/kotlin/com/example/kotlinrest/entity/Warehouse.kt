package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class Warehouse(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
    var code: String by db("")
    var name: String by db("")
    var city: String by db("")
    var country: String by db("")
    var active: Boolean by db(true)
}
