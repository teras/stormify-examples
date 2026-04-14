package com.example.kotlinrest.server.route

import com.example.kotlinrest.server.request.requireIntPath
import com.example.kotlinrest.server.response.respondCsv
import com.example.kotlinrest.server.response.respondNoContent
import com.example.kotlinrest.service.masterdata.CategoryService
import com.example.kotlinrest.service.masterdata.CustomerService
import com.example.kotlinrest.service.masterdata.ProductService
import com.example.kotlinrest.service.masterdata.SupplierService
import com.example.kotlinrest.service.masterdata.WarehouseService
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import onl.ycode.stormify.biglist.PageSpec

internal fun Route.masterDataRoutes(
    categories: CategoryService,
    suppliers: SupplierService,
    customers: CustomerService,
    warehouses: WarehouseService,
    products: ProductService,
) {
    route("/api/categories") {
        post("/search") { call.respond(categories.search(PageSpec.fromJson(call.receiveText()))) }
        post("/export") { call.respondCsv("categories.csv", PageSpec.fromJson(call.receiveText()), "name") { s, w -> categories.exportCsv(s, w) } }
        post { call.respond(categories.create(call.receive())) }
        get("/{id}") { call.respond(categories.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(categories.update(call.requireIntPath("id"), call.receive())) }
        delete("/{id}") {
            categories.delete(call.requireIntPath("id"))
            call.respondNoContent()
        }
    }

    route("/api/suppliers") {
        post("/search") { call.respond(suppliers.search(PageSpec.fromJson(call.receiveText()))) }
        post("/export") { call.respondCsv("suppliers.csv", PageSpec.fromJson(call.receiveText()), "name") { s, w -> suppliers.exportCsv(s, w) } }
        post { call.respond(suppliers.create(call.receive())) }
        get("/{id}") { call.respond(suppliers.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(suppliers.update(call.requireIntPath("id"), call.receive())) }
        delete("/{id}") {
            suppliers.delete(call.requireIntPath("id"))
            call.respondNoContent()
        }
    }

    route("/api/customers") {
        post("/search") { call.respond(customers.search(PageSpec.fromJson(call.receiveText()))) }
        post("/export") { call.respondCsv("customers.csv", PageSpec.fromJson(call.receiveText()), "name") { s, w -> customers.exportCsv(s, w) } }
        post { call.respond(customers.create(call.receive())) }
        get("/{id}") { call.respond(customers.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(customers.update(call.requireIntPath("id"), call.receive())) }
        delete("/{id}") {
            customers.delete(call.requireIntPath("id"))
            call.respondNoContent()
        }
    }

    route("/api/warehouses") {
        post("/search") { call.respond(warehouses.search(PageSpec.fromJson(call.receiveText()))) }
        post("/export") { call.respondCsv("warehouses.csv", PageSpec.fromJson(call.receiveText()), "code") { s, w -> warehouses.exportCsv(s, w) } }
        post { call.respond(warehouses.create(call.receive())) }
        get("/{id}") { call.respond(warehouses.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(warehouses.update(call.requireIntPath("id"), call.receive())) }
        delete("/{id}") {
            warehouses.delete(call.requireIntPath("id"))
            call.respondNoContent()
        }
    }

    route("/api/products") {
        post("/search") { call.respond(products.search(PageSpec.fromJson(call.receiveText()))) }
        post("/export") { call.respondCsv("products.csv", PageSpec.fromJson(call.receiveText()), "sku") { s, w -> products.exportCsv(s, w) } }
        post { call.respond(products.create(call.receive())) }
        get("/{id}") { call.respond(products.getById(call.requireIntPath("id"))) }
        put("/{id}") { call.respond(products.update(call.requireIntPath("id"), call.receive())) }
        delete("/{id}") {
            products.delete(call.requireIntPath("id"))
            call.respondNoContent()
        }
    }
}
