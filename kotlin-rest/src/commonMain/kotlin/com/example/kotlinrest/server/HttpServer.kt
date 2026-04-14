package com.example.kotlinrest.server

import com.example.kotlinrest.config.DatabaseSettings
import com.example.kotlinrest.dto.common.ErrorResponse
import com.example.kotlinrest.exception.ApiException
import com.example.kotlinrest.server.route.inventoryRoutes
import com.example.kotlinrest.server.route.masterDataRoutes
import com.example.kotlinrest.server.route.transactionRoutes
import com.example.kotlinrest.service.inventory.StockService
import com.example.kotlinrest.service.masterdata.CategoryService
import com.example.kotlinrest.service.masterdata.CustomerService
import com.example.kotlinrest.service.masterdata.ProductService
import com.example.kotlinrest.service.masterdata.SupplierService
import com.example.kotlinrest.service.masterdata.WarehouseService
import com.example.kotlinrest.service.transaction.PurchaseOrderService
import com.example.kotlinrest.service.transaction.SalesOrderService
import com.example.kotlinrest.service.transaction.ShipmentService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun startServer(settings: DatabaseSettings) {
    embeddedServer(CIO, port = settings.port, host = settings.host, module = Application::warehouseModule)
        .start(wait = true)
}

fun Application.warehouseModule() {
    install(DefaultHeaders)
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            val status = when (cause.errorCode) {
                "ENTITY_NOT_FOUND" -> HttpStatusCode.NotFound
                else -> HttpStatusCode.BadRequest
            }
            call.respond(status, ErrorResponse(cause.message ?: "Unknown error", cause.errorCode))
        }
        exception<Throwable> { call, cause ->
            val message = cause.message ?: "Unexpected server error"
            val (status, code, friendlyMessage) = when {
                message.contains("FOREIGN KEY constraint failed", ignoreCase = true) ->
                    Triple(HttpStatusCode.Conflict, "FK_CONSTRAINT", "Cannot delete: this record is referenced by other records.")
                else ->
                    Triple(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", message)
            }
            call.respond(status, ErrorResponse(friendlyMessage, code))
        }
    }

    val categories = CategoryService()
    val suppliers = SupplierService()
    val customers = CustomerService()
    val warehouses = WarehouseService()
    val products = ProductService()
    val stock = StockService()
    val purchaseOrders = PurchaseOrderService()
    val salesOrders = SalesOrderService()
    val shipments = ShipmentService()

    routing {
        masterDataRoutes(categories, suppliers, customers, warehouses, products)
        inventoryRoutes(stock)
        transactionRoutes(purchaseOrders, salesOrders, shipments)
    }
}
