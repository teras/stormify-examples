package com.example.kotlinrest.dto.masterdata

import com.example.kotlinrest.dto.common.Ref
import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.dto.common.ref
import com.example.kotlinrest.entity.Product
import kotlinx.serialization.Serializable

@Serializable
data class ProductRequest(
    val sku: String,
    val name: String,
    val description: String,
    val categoryId: Int?,
    val supplierId: Int?,
    val unitPrice: Long,
    val reorderLevel: Int,
    val active: Boolean,
)

@Serializable
data class ProductListItemResponse(
    val id: Int,
    val sku: String,
    val name: String,
    val categoryId: Int?,
    val categoryName: String?,
    val supplierId: Int?,
    val supplierName: String?,
    val unitPrice: Long,
    val active: Boolean,
)

@Serializable
data class ProductDetailsResponse(
    val id: Int,
    val sku: String,
    val name: String,
    val description: String,
    val category: Ref?,
    val supplier: Ref?,
    val unitPrice: Long,
    val reorderLevel: Int,
    val active: Boolean,
)

fun Product.toListItemResponse() = ProductListItemResponse(
    id = pk(id),
    sku = sku,
    name = name,
    categoryId = category?.id,
    categoryName = category?.name,
    supplierId = supplier?.id,
    supplierName = supplier?.name,
    unitPrice = unitPrice,
    active = active,
)

fun Product.toDetailsResponse() = ProductDetailsResponse(
    id = pk(id),
    sku = sku,
    name = name,
    description = description,
    category = ref(category?.id, category?.name),
    supplier = ref(supplier?.id, supplier?.name),
    unitPrice = unitPrice,
    reorderLevel = reorderLevel,
    active = active,
)
