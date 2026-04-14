package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.CreateProductRequest
import com.example.kotlinrest.dto.masterdata.ProductDetailsResponse
import com.example.kotlinrest.dto.masterdata.ProductListItemResponse
import com.example.kotlinrest.dto.masterdata.UpdateProductRequest
import com.example.kotlinrest.entity.Category
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.mapper.toDetailsResponse
import com.example.kotlinrest.mapper.toListItemResponse
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import onl.ycode.stormify.findById
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery

class ProductService {
    private val query = PagedQuery<Product>().apply {
        addFacet("search", "sku", "name", "description").isSortable = false
        addFacet("sku", "sku")
        addFacet("unitPrice", "unitPrice")
        addFacet("reorderLevel", "reorderLevel")
        addFacet("name", "name")
        addFacet("categoryName", "category.name")
        addFacet("supplierName", "supplier.name")
        addFacet("categoryId", "category.id")
        addFacet("supplierId", "supplier.id")
        addFacet("active", mapOf("true" to 1, "false" to 0), "active")
    }

    fun search(spec: PageSpec): PagedResponse<ProductListItemResponse> =
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "sku") { it.toListItemResponse() }

    fun getById(id: Int): ProductDetailsResponse = load(id).toDetailsResponse()

    fun create(request: CreateProductRequest): ProductDetailsResponse {
        validate(request.sku, "Product SKU")
        validate(request.name, "Product name")
        validateNumber(request.unitPrice, "Unit price")
        val product = Product().apply {
            sku = request.sku.trim()
            name = request.name.trim()
            description = request.description.trim()
            category = request.categoryId?.let { loadCategory(it) }
            supplier = request.supplierId?.let { loadSupplier(it) }
            unitPrice = request.unitPrice
            reorderLevel = request.reorderLevel
            active = request.active
        }
        product.create()
        return product.toDetailsResponse()
    }

    fun update(id: Int, request: UpdateProductRequest): ProductDetailsResponse {
        validate(request.sku, "Product SKU")
        validate(request.name, "Product name")
        validateNumber(request.unitPrice, "Unit price")
        val product = load(id).apply {
            sku = request.sku.trim()
            name = request.name.trim()
            description = request.description.trim()
            category = request.categoryId?.let { loadCategory(it) }
            supplier = request.supplierId?.let { loadSupplier(it) }
            unitPrice = request.unitPrice
            reorderLevel = request.reorderLevel
            active = request.active
        }
        product.update()
        return product.toDetailsResponse()
    }

    fun delete(id: Int) {
        Product(id).delete()
    }


    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (ProductListItemResponse) -> Any?>>(
            "id" to { it.id },
            "sku" to { it.sku },
            "name" to { it.name },
            "categoryName" to { it.categoryName },
            "supplierName" to { it.supplierName },
            "unitPrice" to { it.unitPrice },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Product =
        findById<Product>(id) ?: throw EntityNotFoundException("Product", id)

    private fun loadCategory(id: Int): Category =
        findById<Category>(id) ?: throw EntityNotFoundException("Category", id)

    private fun loadSupplier(id: Int): Supplier =
        findById<Supplier>(id) ?: throw EntityNotFoundException("Supplier", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }

    private fun validateNumber(value: Double, label: String) {
        if (value < 0.0) throw ValidationException("$label must not be negative")
    }
}
