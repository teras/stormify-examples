package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CategoryDetailsResponse
import com.example.kotlinrest.dto.masterdata.CategoryListItemResponse
import com.example.kotlinrest.dto.masterdata.CreateCategoryRequest
import com.example.kotlinrest.dto.masterdata.UpdateCategoryRequest
import com.example.kotlinrest.entity.Category
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.mapper.toDetailsResponse
import com.example.kotlinrest.mapper.toListItemResponse
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.findById
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

class CategoryService {
    private val query = PagedQuery<Category>().apply {
        addFacet("search", "name", "description").isSortable = false
        addFacet("name", "name")
        addFacet("description", "description")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    fun search(spec: PageSpec): PagedResponse<CategoryListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "name") { it.toListItemResponse() }

    fun getById(id: Int): CategoryDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateCategoryRequest): CategoryDetailsResponse {
        validate(request.name, "Category name")
        val category = Category().apply {
            name = request.name.trim()
            description = request.description.trim()
            active = request.active
        }
        category.create()
        return category.toDetailsResponse()
    }

    fun update(id: Int, request: UpdateCategoryRequest): CategoryDetailsResponse {
        validate(request.name, "Category name")
        val category = load(id).apply {
            name = request.name.trim()
            description = request.description.trim()
            active = request.active
        }
        category.update()
        return category.toDetailsResponse()
    }

    fun delete(id: Int) {
        Category(id).delete()
    }


    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (CategoryListItemResponse) -> Any?>>(
            "id" to { it.id },
            "name" to { it.name },
            "description" to { it.description },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Category =
        findById<Category>(id) ?: throw EntityNotFoundException("Category", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }
}
