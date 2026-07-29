package com.example.kotlinrest.dto.masterdata

import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.entity.Category
import kotlinx.serialization.Serializable

@Serializable
data class CategoryRequest(
    val name: String,
    val description: String,
    val active: Boolean,
)

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val active: Boolean,
)

fun Category.toResponse() = CategoryResponse(
    id = pk(id),
    name = name,
    description = description,
    active = active,
)
