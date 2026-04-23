package com.example.kotlinrest.entity

import onl.ycode.stormify.*

@DbTable
class Supplier(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable() {
    var name: String by db("")
    var contactName: String by db("")
    var email: String by db("")
    var phone: String by db("")
    var city: String by db("")
    var country: String by db("")
    var active: Boolean by db(true)
}
