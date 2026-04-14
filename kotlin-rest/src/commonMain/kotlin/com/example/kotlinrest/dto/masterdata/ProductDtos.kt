package com.example.kotlinrest.dto.masterdata

import kotlinx.serialization.Serializable

@Serializable
data class CreateProductRequest(
    val sku: String,
    val name: String,
    val description: String,
    val categoryId: Int?,
    val supplierId: Int?,
    val unitPrice: Double,
    val reorderLevel: Int,
    val active: Boolean,
)

@Serializable
data class UpdateProductRequest(
    val sku: String,
    val name: String,
    val description: String,
    val categoryId: Int?,
    val supplierId: Int?,
    val unitPrice: Double,
    val reorderLevel: Int,
    val active: Boolean,
)

@Serializable
data class ReferenceSummaryResponse(
    val id: Int,
    val name: String,
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
    val unitPrice: Double,
    val active: Boolean,
)

@Serializable
data class ProductDetailsResponse(
    val id: Int,
    val sku: String,
    val name: String,
    val description: String,
    val category: ReferenceSummaryResponse?,
    val supplier: ReferenceSummaryResponse?,
    val unitPrice: Double,
    val reorderLevel: Int,
    val active: Boolean,
)
