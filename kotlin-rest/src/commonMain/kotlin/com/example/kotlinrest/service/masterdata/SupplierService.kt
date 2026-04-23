package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CreateSupplierRequest
import com.example.kotlinrest.dto.masterdata.SupplierDetailsResponse
import com.example.kotlinrest.dto.masterdata.SupplierListItemResponse
import com.example.kotlinrest.dto.masterdata.UpdateSupplierRequest
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.mapper.toDetailsResponse
import com.example.kotlinrest.mapper.toListItemResponse
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

class SupplierService {
    private val query = PagedQuery<Supplier>().apply {
        addFacet("search", "name", "contactName", "city", "country").isSortable = false
        addFacet("name", "name")
        addFacet("contactName", "contactName")
        addFacet("city", "city")
        addFacet("country", "country")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    fun search(spec: PageSpec): PagedResponse<SupplierListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "name") { it.toListItemResponse() }

    fun getById(id: Int): SupplierDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateSupplierRequest): SupplierDetailsResponse {
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
        return supplier.toDetailsResponse()
    }

    fun update(id: Int, request: UpdateSupplierRequest): SupplierDetailsResponse {
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
        return supplier.toDetailsResponse()
    }

    fun delete(id: Int) {
        Supplier(id).delete()
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
