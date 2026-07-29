package com.example.kotlinrest.server.route

import com.example.kotlinrest.server.request.receivePageSpec
import com.example.kotlinrest.server.request.requireIntPath
import com.example.kotlinrest.service.transaction.PurchaseOrderService
import com.example.kotlinrest.service.transaction.SalesOrderService
import com.example.kotlinrest.service.transaction.ShipmentService
import com.example.kotlinrest.server.response.respondCreated
import com.example.kotlinrest.server.response.respondCsv
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

internal fun Route.transactionRoutes(
    purchaseOrders: PurchaseOrderService,
    salesOrders: SalesOrderService,
    shipments: ShipmentService,
) {
    route("/api/purchase-orders") {
        post("/search") { call.respond(purchaseOrders.search(call.receivePageSpec())) }
        post("/export") { call.respondCsv("purchase-orders.csv", call.receivePageSpec(), "orderNumber") { s, w -> purchaseOrders.exportCsv(s, w) } }
        post {
            val created = purchaseOrders.create(call.receive())
            call.respondCreated("/api/purchase-orders", created.id, created)
        }
        get("/{id}") { call.respond(purchaseOrders.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(purchaseOrders.update(call.requireIntPath("id"), call.receive())) }
        post("/{id}/receive") { call.respond(purchaseOrders.receive(call.requireIntPath("id"))) }
    }

    route("/api/sales-orders") {
        post("/search") { call.respond(salesOrders.search(call.receivePageSpec())) }
        post("/export") { call.respondCsv("sales-orders.csv", call.receivePageSpec(), "orderNumber") { s, w -> salesOrders.exportCsv(s, w) } }
        post {
            val created = salesOrders.create(call.receive())
            call.respondCreated("/api/sales-orders", created.id, created)
        }
        get("/{id}") { call.respond(salesOrders.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(salesOrders.update(call.requireIntPath("id"), call.receive())) }
        post("/{id}/confirm") { call.respond(salesOrders.confirm(call.requireIntPath("id"))) }
    }

    route("/api/shipments") {
        post("/search") { call.respond(shipments.search(call.receivePageSpec())) }
        post("/export") { call.respondCsv("shipments.csv", call.receivePageSpec(), "shipmentNumber") { s, w -> shipments.exportCsv(s, w) } }
        post {
            val created = shipments.create(call.receive())
            call.respondCreated("/api/shipments", created.id, created)
        }
        get("/{id}") { call.respond(shipments.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(shipments.update(call.requireIntPath("id"), call.receive())) }
        post("/{id}/ship") { call.respond(shipments.ship(call.requireIntPath("id"))) }
    }
}
