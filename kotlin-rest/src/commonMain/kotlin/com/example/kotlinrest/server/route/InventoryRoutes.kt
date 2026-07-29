package com.example.kotlinrest.server.route

import com.example.kotlinrest.server.request.receivePageSpec
import com.example.kotlinrest.server.response.respondCsv
import com.example.kotlinrest.service.inventory.StockService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.inventoryRoutes(stock: StockService) {
    route("/api/stock-items") {
        post("/search") { call.respond(stock.search(call.receivePageSpec())) }
        post("/export") { call.respondCsv("stock.csv", call.receivePageSpec(), "warehouseName") { s, w -> stock.exportCsv(s, w) } }
    }
}
