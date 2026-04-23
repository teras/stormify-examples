package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CreateWarehouseRequest
import com.example.kotlinrest.dto.masterdata.UpdateWarehouseRequest
import com.example.kotlinrest.dto.masterdata.WarehouseDetailsResponse
import com.example.kotlinrest.dto.masterdata.WarehouseListItemResponse
import com.example.kotlinrest.entity.Warehouse
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.mapper.toDetailsResponse
import com.example.kotlinrest.mapper.toListItemResponse
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

class WarehouseService {
    private val query = PagedQuery<Warehouse>().apply {
        addFacet("search", "code", "name", "city", "country").isSortable = false
        addFacet("code", "code")
        addFacet("name", "name")
        addFacet("city", "city")
        addFacet("country", "country")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    fun search(spec: PageSpec): PagedResponse<WarehouseListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "code") { it.toListItemResponse() }

    fun getById(id: Int): WarehouseDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateWarehouseRequest): WarehouseDetailsResponse {
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
        return warehouse.toDetailsResponse()
    }

    fun update(id: Int, request: UpdateWarehouseRequest): WarehouseDetailsResponse {
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
        return warehouse.toDetailsResponse()
    }

    fun delete(id: Int) {
        Warehouse(id).delete()
    }


    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (WarehouseListItemResponse) -> Any?>>(
            "id" to { it.id },
            "code" to { it.code },
            "name" to { it.name },
            "city" to { it.city },
            "country" to { it.country },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Warehouse =
        findById<Warehouse>(id) ?: throw EntityNotFoundException("Warehouse", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }
}
