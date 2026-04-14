package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

/**
 * Master-data entities are also modeled as AutoTable so relations can stay
 * consistent across the whole example project.
 */
@DbTable
class Category(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
    var name: String by db("")
    var description: String by db("")
    var active: Boolean by db(true)
}
