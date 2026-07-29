package com.example.kotlinrest.server.response

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond

internal suspend fun ApplicationCall.respondNoContent() {
    respond(HttpStatusCode.NoContent)
}

/**
 * Answers a successful POST with `201 Created` and a `Location` header pointing at the new
 * row, so a client learns where the thing it just made lives without parsing the body.
 */
internal suspend inline fun <reified T : Any> ApplicationCall.respondCreated(
    path: String,
    id: Int,
    body: T,
) {
    response.header(HttpHeaders.Location, "$path/$id")
    respond(HttpStatusCode.Created, body)
}
