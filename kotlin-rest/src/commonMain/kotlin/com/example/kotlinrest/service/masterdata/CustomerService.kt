package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CustomerDetailsResponse
import com.example.kotlinrest.dto.masterdata.CustomerListItemResponse
import com.example.kotlinrest.dto.masterdata.CustomerRequest
import com.example.kotlinrest.dto.masterdata.toDetailsResponse
import com.example.kotlinrest.dto.masterdata.toListItemResponse
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.CustomerType
import com.example.kotlinrest.db.catchingConstraints
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.Facet
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.coroutines.SuspendStormify

internal class CustomerService(private val async: SuspendStormify) {
    private val query = PagedQuery<Customer>().apply {
        addFacet("search", "name", "email", "city", "country").isSortable = false
        addFacet("name", "name")
        addFacet("email", "email")
        addFacet("city", "city")
        addFacet("country", "country")
        // `enumAsString` stores the name, so the column is already the text to filter on.
        addSqlFacet("customerType", "customer.customer_type", Facet.TEXT)
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    suspend fun search(spec: PageSpec): PagedResponse<CustomerListItemResponse> = async.withConnection {
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "name") { it.toListItemResponse() }
    }

    suspend fun getById(id: Int): CustomerDetailsResponse = async.withConnection { load(id).toDetailsResponse() }

    suspend fun create(request: CustomerRequest): CustomerDetailsResponse = async.withConnection {
        validate(request.name, "Customer name")
        validate(request.email, "Customer email")
        validate(request.phone, "Customer phone")
        validate(request.city, "Customer city")
        validate(request.country, "Customer country")
        val customer = Customer().apply {
            name = request.name.trim()
            email = request.email.trim()
            phone = request.phone.trim()
            city = request.city.trim()
            country = request.country.trim()
            customerType = request.customerType
            active = request.active
        }
        customer.create()
        customer.toDetailsResponse()
    }

    suspend fun update(id: Int, request: CustomerRequest): CustomerDetailsResponse = async.withConnection {
        validate(request.name, "Customer name")
        validate(request.email, "Customer email")
        validate(request.phone, "Customer phone")
        validate(request.city, "Customer city")
        validate(request.country, "Customer country")
        val customer = load(id).apply {
            name = request.name.trim()
            email = request.email.trim()
            phone = request.phone.trim()
            city = request.city.trim()
            country = request.country.trim()
            customerType = request.customerType
            active = request.active
        }
        customer.update()
        customer.toDetailsResponse()
    }

    suspend fun delete(id: Int): Unit = async.withConnection {
        // Look the row up first: deleting by a bare id reports success even when
        // nothing matched, so a wrong id would answer 204 instead of 404.
        val customer = load(id)
        catchingConstraints("This customer is still referenced and cannot be deleted") {
            customer.delete()
        }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (CustomerListItemResponse) -> Any?>>(
            "id" to { it.id },
            "name" to { it.name },
            "email" to { it.email },
            "city" to { it.city },
            "country" to { it.country },
            "customerType" to { it.customerType },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Customer =
        findById<Customer>(id) ?: throw EntityNotFoundException("Customer", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }
}
