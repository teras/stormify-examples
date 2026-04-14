package com.example.kotlinrest.server.response

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

internal suspend fun ApplicationCall.respondNoContent() {
    respond(HttpStatusCode.NoContent)
}
