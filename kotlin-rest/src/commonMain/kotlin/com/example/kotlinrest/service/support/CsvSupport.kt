package com.example.kotlinrest.service.support

/**
 * CSV formatting for the exports. The services stream their rows themselves through
 * `PagedQuery.forEachStreaming`, writing the [header] first and then one [row] per entity
 * as the query yields it, so an export of any size costs a bounded amount of memory.
 */
internal object CsvSupport {

    /** Rows held back before a chunk is mapped. Bounds memory; nothing else depends on it. */
    private const val CHUNK = 500

    fun <R> header(columns: List<Pair<String, (R) -> Any?>>): String =
        columns.joinToString(",") { csvField(it.first) }

    fun <R> row(columns: List<Pair<String, (R) -> Any?>>, mapped: R): String =
        columns.joinToString(",") { (_, extract) -> csvField(extract(mapped)) }

    /**
     * Collects streamed rows into chunks, for rows that need a lookup the query did not select.
     *
     * Streaming and batching pull in opposite directions - one wants a row at a time, the
     * other wants many at once - so rows are collected into chunks: each chunk is mapped
     * in one go, which is what lets the mapper issue a single query for all of them, and
     * memory still never holds more than a chunk. Call [flush] once the stream has ended.
     */
    class Chunks<T, R>(
        private val columns: List<Pair<String, (R) -> Any?>>,
        private val writeLine: (String) -> Unit,
        private val mapper: (List<T>) -> List<R>,
    ) {
        private val buffer = ArrayList<T>(CHUNK)

        fun add(item: T) {
            buffer.add(item)
            if (buffer.size >= CHUNK) flush()
        }

        fun flush() {
            if (buffer.isEmpty()) return
            for (mapped in mapper(buffer)) writeLine(row(columns, mapped))
            buffer.clear()
        }
    }

    /**
     * Quotes per RFC 4180, and defuses the spreadsheet formula trick: a cell opening with
     * `=`, `+`, `-` or `@` is executed on open by Excel and Calc, so a value like
     * `=1+1` - or something far worse aimed at a colleague who opens the export - is
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
