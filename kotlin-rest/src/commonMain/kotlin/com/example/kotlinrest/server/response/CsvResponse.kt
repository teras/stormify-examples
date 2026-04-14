package com.example.kotlinrest.server.response

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.SortDir

/**
 * Writes a CSV download response for a filterable export. The caller provides
 * a block that produces CSV lines (via `writeLine`) from the effective
 * [PageSpec]; the helper assembles them, sets the right headers, and returns
 * the plain-text payload.
 */
internal suspend fun ApplicationCall.respondCsv(
    fileName: String,
    spec: PageSpec,
    defaultSortAlias: String,
    block: (PageSpec, (String) -> Unit) -> Unit,
) {
    val effective = if (spec.sorts.isEmpty()) spec.copy(sorts = mapOf(defaultSortAlias to SortDir.ASC)) else spec
    val buffer = StringBuilder()
    block(effective) { line ->
        buffer.append(line).append('\n')
    }
    response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$fileName\"")
    respondText(buffer.toString(), contentType = ContentType("text", "csv"))
}
