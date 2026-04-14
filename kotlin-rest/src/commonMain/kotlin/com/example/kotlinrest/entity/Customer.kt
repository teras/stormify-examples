package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

@DbTable
class Customer(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
    var name: String by db("")
    var email: String by db("")
    var phone: String by db("")
    var city: String by db("")
    var country: String by db("")
    var customerType: CustomerType by db(CustomerType.RETAIL)
    var active: Boolean by db(true)
}
