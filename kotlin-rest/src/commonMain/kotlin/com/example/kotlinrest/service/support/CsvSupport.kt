package com.example.kotlinrest.service.support

import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

/**
 * Streams a paged query to CSV through [PagedQuery.forEachStreaming]: the header goes out
 * first, then each row as the query yields it, so an export of any size costs a bounded
 * amount of memory.
 */
internal object CsvSupport {

    /** Rows held back before a chunk is mapped. Bounds memory; nothing else depends on it. */
    private const val CHUNK = 500

    fun <T : Any, R> stream(
        query: PagedQuery<T>,
        spec: PageSpec,
        columns: List<Pair<String, (R) -> Any?>>,
        mapper: (T) -> R,
        writeLine: (String) -> Unit,
    ) = streamChunked(query, spec, columns, { rows -> rows.map(mapper) }, writeLine)

    /**
     * The variant for rows that need a lookup the query did not select.
     *
     * Streaming and batching pull in opposite directions — one wants a row at a time, the
     * other wants many at once — so rows are collected into chunks: each chunk is mapped
     * in one go, which is what lets the mapper issue a single query for all of them, and
     * memory still never holds more than a chunk.
     */
    fun <T : Any, R> streamChunked(
        query: PagedQuery<T>,
        spec: PageSpec,
        columns: List<Pair<String, (R) -> Any?>>,
        mapper: (List<T>) -> List<R>,
        writeLine: (String) -> Unit,
    ) {
        writeLine(columns.joinToString(",") { csvField(it.first) })
        val buffer = ArrayList<T>(CHUNK)
        fun flush() {
            if (buffer.isEmpty()) return
            for (mapped in mapper(buffer))
                writeLine(columns.joinToString(",") { (_, extract) -> csvField(extract(mapped)) })
            buffer.clear()
        }
        // page 0 with a large page size: forEachStreaming walks the whole result set and
        // the pagination fields are ignored on this path.
        query.forEachStreaming(spec.copy(page = 0, pageSize = 1000)) { row ->
            buffer.add(row)
            if (buffer.size >= CHUNK) flush()
        }
        flush()
    }

    /**
     * Quotes per RFC 4180, and defuses the spreadsheet formula trick: a cell opening with
     * `=`, `+`, `-` or `@` is executed on open by Excel and Calc, so a value like
     * `=1+1` — or something far worse aimed at a colleague who opens the export — is
     * prefixed with an apostrophe to keep it text.
     */
    private fun csvField(value: Any?): String {
        var raw = value?.toString() ?: ""
        if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") raw = "'$raw"
        val needsQuote = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return raw
        return "\"" + raw.replace("\"", "\"\"") + "\""
    }
}
