package com.example.kotlinrest.service.support

import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

/**
 * Streams a paged query to CSV via [PagedQuery.forEachStreaming]. Header row is
 * written first; each data row is sent through [writeLine] as the underlying
 * query yields it, so the response can be piped straight to a client without
 * loading all rows into memory.
 */
internal object CsvSupport {
    fun <T : Any, R> stream(
        query: PagedQuery<T>,
        spec: PageSpec,
        columns: List<Pair<String, (R) -> Any?>>,
        mapper: (T) -> R,
        writeLine: (String) -> Unit,
    ) {
        writeLine(columns.joinToString(",") { csvField(it.first) })
        val streamingSpec = spec.copy(page = 0, pageSize = 1000)
        query.forEachStreaming(streamingSpec) { row ->
            val mapped = mapper(row)
            writeLine(columns.joinToString(",") { (_, extract) -> csvField(extract(mapped)) })
        }
    }

    private fun csvField(value: Any?): String {
        val raw = value?.toString() ?: ""
        val needsQuote = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuote) return raw
        return "\"" + raw.replace("\"", "\"\"") + "\""
    }
}
