package com.example.kotlinrest.entity

import onl.ycode.stormify.AutoTable
import onl.ycode.stormify.CRUDTable
import onl.ycode.stormify.DbField
import onl.ycode.stormify.DbTable
import onl.ycode.stormify.db

@DbTable
class Shipment(
    @DbField(primaryKey = true, autoIncrement = true)
    var id: Int? = null,
) : AutoTable(), CRUDTable {
    var shipmentNumber: String by db("")
    @DbField(name = "sales_order_id")
    var salesOrder: SalesOrder? by db(null)
    @DbField(name = "warehouse_id")
    var warehouse: Warehouse? by db(null)
    var status: ShipmentStatus by db(ShipmentStatus.PREPARING)
    var carrier: String by db("")
    var trackingCode: String by db("")
    var shippedAt: String by db("")
    var deliveredAt: String by db("")
}
