package com.example.kotlinrest.dto.masterdata

import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val description: String,
    val active: Boolean,
)

@Serializable
data class UpdateCategoryRequest(
    val name: String,
    val description: String,
    val active: Boolean,
)

@Serializable
data class CategoryListItemResponse(
    val id: Int,
    val name: String,
    val description: String,
    val active: Boolean,
)

@Serializable
data class CategoryDetailsResponse(
    val id: Int,
    val name: String,
    val description: String,
    val active: Boolean,
)
