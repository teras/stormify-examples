package com.example.kotlinrest.service.masterdata

import com.example.kotlinrest.dto.common.PagedResponse
import com.example.kotlinrest.dto.masterdata.ProductDetailsResponse
import com.example.kotlinrest.dto.masterdata.ProductListItemResponse
import com.example.kotlinrest.dto.masterdata.ProductRequest
import com.example.kotlinrest.dto.masterdata.toDetailsResponse
import com.example.kotlinrest.dto.masterdata.toListItemResponse
import com.example.kotlinrest.entity.Category
import com.example.kotlinrest.entity.Product
import com.example.kotlinrest.entity.Supplier
import com.example.kotlinrest.db.catchingConstraints
import com.example.kotlinrest.exception.EntityNotFoundException
import com.example.kotlinrest.exception.ReferenceNotFoundException
import com.example.kotlinrest.exception.ValidationException
import com.example.kotlinrest.service.support.CsvSupport
import com.example.kotlinrest.service.support.PagedQuerySupport
import com.example.kotlinrest.support.centsToDecimal
import onl.ycode.stormify.*
import onl.ycode.stormify.biglist.PageSpec
import onl.ycode.stormify.biglist.PagedQuery
import onl.ycode.stormify.coroutines.SuspendStormify

internal class ProductService(private val async: SuspendStormify) {
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

    suspend fun search(spec: PageSpec): PagedResponse<ProductListItemResponse> = async.withConnection {
        PagedQuerySupport.execute(query, spec, defaultSortAlias = "sku") { it.toListItemResponse() }
    }

    suspend fun getById(id: Int): ProductDetailsResponse = async.withConnection { load(id).toDetailsResponse() }

    suspend fun create(request: ProductRequest): ProductDetailsResponse = async.withConnection {
        validate(request.sku, "Product SKU")
        validate(request.name, "Product name")
        validateNumber(request.unitPrice, "Unit price")
        validateReorderLevel(request.reorderLevel)
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
        product.toDetailsResponse()
    }

    suspend fun update(id: Int, request: ProductRequest): ProductDetailsResponse = async.withConnection {
        validate(request.sku, "Product SKU")
        validate(request.name, "Product name")
        validateNumber(request.unitPrice, "Unit price")
        validateReorderLevel(request.reorderLevel)
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
        product.toDetailsResponse()
    }

    suspend fun delete(id: Int): Unit = async.withConnection {
        // Look the row up first: deleting by a bare id reports success even when
        // nothing matched, so a wrong id would answer 204 instead of 404.
        val product = load(id)
        catchingConstraints("This product is still referenced and cannot be deleted") {
            product.delete()
        }
    }

    fun exportCsv(spec: PageSpec, writeLine: (String) -> Unit) {
        val columns = listOf<Pair<String, (ProductListItemResponse) -> Any?>>(
            "id" to { it.id },
            "sku" to { it.sku },
            "name" to { it.name },
            "categoryName" to { it.categoryName },
            "supplierName" to { it.supplierName },
            "unitPrice" to { centsToDecimal(it.unitPrice) },
            "active" to { it.active }
        )
        CsvSupport.stream(query, spec, columns, mapper = { it.toListItemResponse() }, writeLine = writeLine)
    }

    private fun load(id: Int): Product =
        findById<Product>(id) ?: throw EntityNotFoundException("Product", id)

    private fun loadCategory(id: Int): Category =
        findById<Category>(id) ?: throw ReferenceNotFoundException("Category", "categoryId", id)

    private fun loadSupplier(id: Int): Supplier =
        findById<Supplier>(id) ?: throw ReferenceNotFoundException("Supplier", "supplierId", id)

    private fun validate(value: String, label: String) {
        if (value.isBlank()) throw ValidationException("$label must not be blank")
    }

    private fun validateNumber(value: Long, label: String) {
        if (value < 0L) throw ValidationException("$label must not be negative")
    }

    private fun validateReorderLevel(value: Int) {
        if (value < 0) throw ValidationException("Reorder level must not be negative", "reorderLevel")
    }
}
