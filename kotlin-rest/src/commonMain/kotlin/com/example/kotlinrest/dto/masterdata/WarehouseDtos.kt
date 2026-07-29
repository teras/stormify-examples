package com.example.kotlinrest.dto.masterdata

import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.entity.Warehouse
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseRequest(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class WarehouseResponse(
    val id: Int,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

fun Warehouse.toResponse() = WarehouseResponse(
    id = pk(id),
    code = code,
    name = name,
    city = city,
    country = country,
    active = active,
)
