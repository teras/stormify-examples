package com.example.kotlinrest.service.support

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.exception.ValidationException
import onl.ycode.stormify.biglist.Page
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.biglist.SortDir

internal object PagedQuerySupport {
    fun validate(spec: PageSpec) {
        if (spec.page < 0) throw ValidationException("Page index must be zero or positive")
        if (spec.pageSize !in 1..200) throw ValidationException("Page size must be between 1 and 200")
    }

    fun normalizedSpec(spec: PageSpec, defaultSortAlias: String? = null): PageSpec {
        validate(spec)
        if (!spec.sorts.isEmpty() || defaultSortAlias == null) return spec
        return spec.copy(sorts = mapOf(defaultSortAlias to SortDir.ASC))
    }

    /**
     * Fails loudly when a service names a default sort that the query does not have.
     *
     * A sort alias is just a string, so a rename or a typo makes the default sort quietly
     * do nothing — every page comes back in whatever order the database felt like, and no
     * test notices because the rows are all still there. Checking it where the service
     * declares it turns that into an error at the first request instead.
     */
    private fun <T : Any> requireSortAlias(query: PagedQuery<T>, alias: String?) {
        if (alias == null) return
        if (query.facets.none { it.alias == alias })
            error("Default sort alias '$alias' is not a facet of this query. " +
                "Available: ${query.facets.joinToString { it.alias }}")
    }

    fun <T : Any, R> execute(
        query: PagedQuery<T>,
        spec: PageSpec,
        defaultSortAlias: String? = null,
        mapper: (T) -> R,
    ): PagedResponse<R> {
        requireSortAlias(query, defaultSortAlias)
        val page = query.execute(normalizedSpec(spec, defaultSortAlias))
        return buildResponse(page, mapper)
    }

    /**
     * Same as [execute], but the whole page is handed to [mapper] at once.
     *
     * Some rows need something the query did not select — the order totals, say. Mapping
     * row by row would fetch that per row; mapping the page lets the caller fetch it for
     * all of them in one query.
     */
    fun <T : Any, R> executePage(
        query: PagedQuery<T>,
        spec: PageSpec,
        defaultSortAlias: String? = null,
        mapper: (List<T>) -> List<R>,
    ): PagedResponse<R> {
        requireSortAlias(query, defaultSortAlias)
        val page = query.execute(normalizedSpec(spec, defaultSortAlias))
        return PagedResponse(
            items = mapper(page.rows),
            page = page.page,
            pageSize = page.pageSize,
            totalItems = page.total,
            totalPages = page.totalPages,
        )
    }

    fun <T, R> buildResponse(page: Page<T>, mapper: (T) -> R): PagedResponse<R> =
        PagedResponse(
            items = page.rows.map(mapper),
            page = page.page,
            pageSize = page.pageSize,
            totalItems = page.total,
            totalPages = page.totalPages,
        )
}
