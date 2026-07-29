package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.SupplierDetailsResponse
import com.example.kotlinrest.dto.masterdata.SupplierListItemResponse
import com.example.kotlinrest.dto.masterdata.SupplierRequest
import com.example.kotlinrest.dto.masterdata.toDetailsResponse
import com.example.kotlinrest.dto.masterdata.toListItemResponse
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.db.catchingConstraints
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.coroutines.SuspendStormify

internal class SupplierService(private val async: SuspendStormify) {
    private val query = PagedQuery<Supplier>().apply {
        addFacet("search", "name", "contactName", "city", "country").isSortable = false
        addFacet("name", "name")
        addFacet("contactName", "contactName")
        addFacet("city", "city")
        addFacet("country", "country")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    suspend fun search(spec: PageSpec): PagedResponse<SupplierListItemResponse> = async.withConnection {
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "name") { it.toListItemResponse() }
    }

    suspend fun getById(id: Int): SupplierDetailsResponse = async.withConnection { load(id).toDetailsResponse() }

    suspend fun create(request: SupplierRequest): SupplierDetailsResponse = async.withConnection {
        validate(request.name, "Supplier name")
        validate(request.contactName, "Supplier contact name")
        validate(request.email, "Supplier email")
        validate(request.phone, "Supplier phone")
        validate(request.city, "Supplier city")
        validate(request.country, "Supplier country")
        val supplier = Supplier().apply {
            name = request.name.trim()
            contactName = request.contactName.trim()
            email = request.email.trim()
            phone = request.phone.trim()
            city = request.city.trim()
            country = request.country.trim()
            active = request.active
        }
        supplier.create()
        supplier.toDetailsResponse()
    }

    suspend fun update(id: Int, request: SupplierRequest): SupplierDetailsResponse = async.withConnection {
        validate(request.name, "Supplier name")
        validate(request.contactName, "Supplier contact name")
        validate(request.email, "Supplier email")
        validate(request.phone, "Supplier phone")
        validate(request.city, "Supplier city")
        validate(request.country, "Supplier country")
        val supplier = load(id).apply {
            name = request.name.trim()
            contactName = request.contactName.trim()
            email = request.email.trim()
            phone = request.phone.trim()
            city = request.city.trim()
            country = request.country.trim()
            active = request.active
        }
        supplier.update()
        supplier.toDetailsResponse()
    }

    suspend fun delete(id: Int): Unit = async.withConnection {
        // Look the row up first: deleting by a bare id reports success even when
        // nothing matched, so a wrong id would answer 204 instead of 404.
        val supplier = load(id)
        catchingConstraints("This supplier is still referenced and cannot be deleted") {
            supplier.delete()
        }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (SupplierListItemResponse) -> Any?>>(
            "id" to { it.id },
            "name" to { it.name },
            "contactName" to { it.contactName },
            "city" to { it.city },
            "country" to { it.country },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Supplier =
        findById<Supplier>(id) ?: throw EntityNotFoundException("Supplier", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }
}
