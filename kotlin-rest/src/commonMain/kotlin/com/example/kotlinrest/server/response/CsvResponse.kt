package com.example.kotlinrest.server.response

import com.example.kotlinrest.service.support.PagedQuerySupport
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respondBytesWriter
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.runBlocking
import onl.ycode.stormify.biglist.PageSpec

/** Bytes held before a flush. Bounds memory; nothing else depends on the value. */
private const val FLUSH_THRESHOLD = 32 * 1024

/**
 * Streams a CSV download.
 *
 * The rows go out as they are produced rather than being assembled first, so an export
 * costs a bounded amount of memory whatever its size — which is the point of having a
 * streaming query underneath it.
 *
 * Line endings are CRLF and the charset is stated explicitly, both per RFC 4180: a bare
 * LF and an unstated charset are the two things that make an export open wrong in a
 * spreadsheet on somebody else's machine.
 */
internal suspend fun ApplicationCall.respondCsv(
    fileName: String,
    spec: PageSpec,
    defaultSortAlias: String,
    block: (PageSpec, (String) -> Unit) -> Unit,
) {
    // One place decides the default sort. This used to restate the rule and had already
    // drifted from the copy in PagedQuerySupport.
    val effective = PagedQuerySupport.normalizedSpec(spec, defaultSortAlias)
    response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$fileName\"")
    respondBytesWriter(ContentType("text", "csv").withCharset(Charsets.UTF_8)) {
        val buffer = StringBuilder(FLUSH_THRESHOLD)
        block(effective) { line ->
            buffer.append(line).append("\r\n")
            if (buffer.length >= FLUSH_THRESHOLD) {
                // The query drives this callback synchronously, so the flush cannot
                // suspend here. Blocking is acceptable because the row that produced the
                // line was itself fetched by a blocking read on this same thread.
                runBlocking { writeStringUtf8(buffer.toString()) }
                buffer.setLength(0)
            }
        }
        if (buffer.isNotEmpty()) writeStringUtf8(buffer.toString())
    }
}
