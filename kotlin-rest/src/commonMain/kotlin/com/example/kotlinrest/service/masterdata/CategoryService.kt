package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CategoryRequest
import com.example.kotlinrest.dto.masterdata.CategoryResponse
import com.example.kotlinrest.dto.masterdata.toResponse
import com.example.kotlinrest.entity.Category
import com.example.kotlinrest.db.catchingConstraints
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.coroutines.SuspendStormify

internal class CategoryService(private val async: SuspendStormify) {
    private val query = PagedQuery<Category>().apply {
        addFacet("search", "name", "description").isSortable = false
        addFacet("name", "name")
        addFacet("description", "description")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    suspend fun search(spec: PageSpec): PagedResponse<CategoryResponse> = async.withConnection {
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "name") { it.toResponse() }
    }

    suspend fun getById(id: Int): CategoryResponse = async.withConnection { load(id).toResponse() }

    suspend fun create(request: CategoryRequest): CategoryResponse = async.withConnection {
        validate(request.name, "Category name")
        val category = Category().apply {
            name = request.name.trim()
            description = request.description.trim()
            active = request.active
        }
        category.create()
        category.toResponse()
    }

    suspend fun update(id: Int, request: CategoryRequest): CategoryResponse = async.withConnection {
        validate(request.name, "Category name")
        val category = load(id).apply {
            name = request.name.trim()
            description = request.description.trim()
            active = request.active
        }
        category.update()
        category.toResponse()
    }

    suspend fun delete(id: Int): Unit = async.withConnection {
        // Look the row up first: deleting by a bare id reports success even when
        // nothing matched, so a wrong id would answer 204 instead of 404.
        val category = load(id)
        catchingConstraints("This category is still referenced and cannot be deleted") {
            category.delete()
        }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (CategoryResponse) -> Any?>>(
            "id" to { it.id },
            "name" to { it.name },
            "description" to { it.description },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Category =
        findById<Category>(id) ?: throw EntityNotFoundException("Category", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }
}
