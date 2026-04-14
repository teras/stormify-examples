package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CreateCustomerRequest
import com.example.kotlinrest.dto.masterdata.CustomerDetailsResponse
import com.example.kotlinrest.dto.masterdata.CustomerListItemResponse
import com.example.kotlinrest.dto.masterdata.UpdateCustomerRequest
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.CustomerType
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.mapper.toDetailsResponse
import com.example.kotlinrest.mapper.toListItemResponse
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.addEnumFacet
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.findById
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

class CustomerService {
    private val query = PagedQuery<Customer>().apply {
        addFacet("search", "name", "email", "city", "country").isSortable = false
        addFacet("name", "name")
        addFacet("email", "email")
        addFacet("city", "city")
        addFacet("country", "country")
        addEnumFacet<CustomerType>("customerType", "customer.customer_type")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    fun search(spec: PageSpec): PagedResponse<CustomerListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "name") { it.toListItemResponse() }

    fun getById(id: Int): CustomerDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateCustomerRequest): CustomerDetailsResponse {
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
        return customer.toDetailsResponse()
    }

    fun update(id: Int, request: UpdateCustomerRequest): CustomerDetailsResponse {
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
        return customer.toDetailsResponse()
    }

    fun delete(id: Int) {
        Customer(id).delete()
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
