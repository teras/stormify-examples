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

    fun <T : Any, R> execute(
        query: PagedQuery<T>,
        spec: PageSpec,
        defaultSortAlias: String? = null,
        mapper: (T) -> R,
    ): PagedResponse<R> {
        val page = query.execute(normalizedSpec(spec, defaultSortAlias))
        return buildResponse(page, mapper)
    }

    fun <T, R> buildResponse(page: Page<T>, mapper: (T) -> R): PagedResponse<R> =
        PagedResponse(
            items = page.rows.map(mapper),
            page = page.page,
            size = page.pageSize,
            totalItems = page.total,
            totalPages = page.totalPages,
        )
}
