package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.WarehouseRequest
import com.example.kotlinrest.dto.masterdata.WarehouseResponse
import com.example.kotlinrest.dto.masterdata.toResponse
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.db.catchingConstraints
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.coroutines.SuspendStormify

internal class WarehouseService(private val async: SuspendStormify) {
    private val query = async.stormify.pagedQuery<Warehouse>().apply {
        addFacet("search", "code", "name", "city", "country").isSortable = false
        addFacet("code", "code")
        addFacet("name", "name")
        addFacet("city", "city")
        addFacet("country", "country")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    suspend fun search(spec: PageSpec): PagedResponse<WarehouseResponse> = async.withConnection {
        val page = query.execute(PagedQuerySupport.searchSpec(query, spec, defaultSortAlias = "code"))
        PagedQuerySupport.buildResponse(page) { it.toResponse() }
    }

    suspend fun getById(id: Int): WarehouseResponse = async.withConnection { load(id).toResponse() }

    suspend fun create(request: WarehouseRequest): WarehouseResponse = async.withConnection {
        validate(request.code, "Warehouse code")
        validate(request.name, "Warehouse name")
        validate(request.city, "Warehouse city")
        validate(request.country, "Warehouse country")
        val warehouse = Warehouse().apply {
            code = request.code.trim()
            name = request.name.trim()
            city = request.city.trim()
            country = request.country.trim()
            active = request.active
        }
        warehouse.create()
        warehouse.toResponse()
    }

    suspend fun update(id: Int, request: WarehouseRequest): WarehouseResponse = async.withConnection {
        validate(request.code, "Warehouse code")
        validate(request.name, "Warehouse name")
        validate(request.city, "Warehouse city")
        validate(request.country, "Warehouse country")
        val warehouse = load(id).apply {
            code = request.code.trim()
            name = request.name.trim()
            city = request.city.trim()
            country = request.country.trim()
            active = request.active
        }
        warehouse.update()
        warehouse.toResponse()
    }

    suspend fun delete(id: Int): Unit = async.withConnection {
        // Look the row up first: deleting by a bare id reports success even when
        // nothing matched, so a wrong id would answer 204 instead of 404.
        val warehouse = load(id)
        catchingConstraints("This warehouse is still referenced and cannot be deleted") {
            warehouse.delete()
        }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (WarehouseResponse) -> Any?>>(
            "id" to { it.id },
            "code" to { it.code },
            "name" to { it.name },
            "city" to { it.city },
            "country" to { it.country },
            "active" to { it.active }
        )
        writeLine(CsvSupport.header(columns))
        // forEachStreaming walks the whole result set, so the pagination fields of the
        // spec are ignored on this path: page 0 with a large page size.
        query.forEachStreaming(spec.copy(page = 0, pageSize = 1000)) { writeLine(CsvSupport.row(columns, it.toResponse())) }
    }

    private fun load(id: Int): Warehouse =
        findById<Warehouse>(id) ?: throw EntityNotFoundException("Warehouse", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }
}
