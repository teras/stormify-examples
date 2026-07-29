package com.example.kotlinrest.server

import com.example.kotlinrest.config.AppSettings
import com.example.kotlinrest.dto.common.ErrorResponse
import com.example.kotlinrest.dto.common.HealthResponse
import com.example.kotlinrest.dto.common.toResponse
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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import onl.ycode.stormify.Stormify

/** A search or write body larger than this is refused before it is read. */
private const val MAX_BODY_BYTES = 1L * 1024 * 1024

private suspend fun ApplicationCall.respondError(e: ApiException) =
    respond(e.status, ErrorResponse(e.message ?: "Unknown error", e.errorCode, e.details))

internal fun startServer(settings: AppSettings, stormify: Stormify) {
    embeddedServer(
        CIO,
        port = settings.port,
        host = settings.host,
        module = { warehouseModule(settings, stormify) },
    ).start(wait = true)
}

internal fun Application.warehouseModule(settings: AppSettings, stormify: Stormify) {
    val async = stormify.suspending

    // On Native every pooled connection owns a thread, so the pool must be closed at
    // shutdown or the process leaks threads, not just connections. closeSuspending is a
    // suspend call and this hook is not, hence runBlocking.
    monitor.subscribe(ApplicationStopped) {
        runBlocking { stormify.closeSuspending() }
    }

    // Refuse an oversized body before the serializer reads it: a declared Content-Length
    // over the limit is answered 413 up front instead of buffering megabytes first.
    intercept(ApplicationCallPipeline.Setup) {
        val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (declaredLength != null && declaredLength > MAX_BODY_BYTES) {
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ErrorResponse("Request body exceeds ${MAX_BODY_BYTES / 1024} KB", "PAYLOAD_TOO_LARGE"),
            )
            finish()
        }
    }

    install(DefaultHeaders)
    install(AutoHeadResponse)
    // CallLogging and Compression are not installed: neither ktor plugin publishes a
    // Kotlin/Native artifact, and this server is a native binary.
    install(CORS) {
        // Demo only. A deployed service names its front end here instead of accepting
        // every origin, and the same goes for binding 0.0.0.0 above.
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
        json(Json { prettyPrint = settings.prettyPrint; ignoreUnknownKeys = true })
    }
    install(StatusPages) {
        exception<ApiException> { call, cause -> call.respondError(cause) }

        // A body that is not the JSON we asked for is the caller's mistake, not ours.
        // Without these three the serializer's own exception escapes to the catch-all
        // below and a typo in a request reads as a server failure.
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Malformed request body", "BAD_REQUEST"),
            )
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ErrorResponse("Expected Content-Type: application/json", "UNSUPPORTED_MEDIA_TYPE"),
            )
        }
        // PageSpec.fromJson and the facet lookups report misuse this way.
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Bad request", "BAD_REQUEST"),
            )
        }

        exception<Throwable> { call, cause ->
            // A transaction rolling back rethrows wrapped, so the deliberate error is
            // one level down. Unwrap it rather than reporting a write failure as a crash.
            val api = cause as? ApiException ?: cause.cause as? ApiException
            if (api != null) return@exception call.respondError(api)

            // Log what actually happened; answer with something that leaks nothing —
            // driver messages carry SQL and column names.
            call.application.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Unexpected server error", "INTERNAL_ERROR"),
            )
        }

        // ktor answers an unknown route in plain text, which breaks the JSON contract
        // for exactly the clients most likely to hit it.
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Not found", "NOT_FOUND"))
        }
    }

    val categories = CategoryService(async)
    val suppliers = SupplierService(async)
    val customers = CustomerService(async)
    val warehouses = WarehouseService(async)
    val products = ProductService(async)
    val stock = StockService(async)
    val purchaseOrders = PurchaseOrderService(async)
    val salesOrders = SalesOrderService(async)
    val shipments = ShipmentService(async)

    routing {
        get("/api/health") { call.respond(HealthResponse("ok", async.stats.toResponse())) }
        masterDataRoutes(categories, suppliers, customers, warehouses, products)
        inventoryRoutes(stock)
        transactionRoutes(purchaseOrders, salesOrders, shipments)
    }
}
