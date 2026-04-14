package com.example.kotlinrest.server.route

import com.example.kotlinrest.service.inventory.StockService
import com.example.kotlinrest.server.response.respondCsv
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import onl.ycode.stormify.biglist.PageSpec

internal fun Route.inventoryRoutes(stock: StockService) {
    route("/api/stock-items") {
        post("/search") { call.respond(stock.search(PageSpec.fromJson(call.receiveText()))) }
        post("/export") { call.respondCsv("stock.csv", PageSpec.fromJson(call.receiveText()), "warehouseName") { s, w -> stock.exportCsv(s, w) } }
    }
}
