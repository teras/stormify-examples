package com.example.kotlinrest.server.request

import com.example.kotlinrest.exception.ValidationException
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import onl.ycode.stormify.biglist.PageSpec

internal fun ApplicationCall.requireIntPath(name: String): Int {
    val value = parameters[name] ?: throw ValidationException("Missing path parameter '$name'")
    return value.toIntOrNull() ?: throw ValidationException("Path parameter '$name' must be an integer")
}

/**
 * Reads the search/export body as a [PageSpec].
 *
 * `PageSpec` is parsed from raw text rather than through content negotiation, because its
 * shape belongs to the library rather than to this application. That skips two checks the
 * negotiation plugin would have made — the content type, and an empty body — so they are
 * made here, once, instead of reaching the parser and surfacing as something that reads
 * like a server fault.
 */
internal suspend fun ApplicationCall.receivePageSpec(): PageSpec {
    val type = request.contentType()
    if (!type.match(ContentType.Application.Json)) throw UnsupportedMediaTypeException(type)
    val body = receiveText()
    if (body.isBlank()) throw ValidationException("A search body is required")
    return PageSpec.fromJson(body)
}
