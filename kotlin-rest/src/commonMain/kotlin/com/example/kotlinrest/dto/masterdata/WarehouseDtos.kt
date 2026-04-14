package com.example.kotlinrest.dto.masterdata

import kotlinx.serialization.Serializable

@Serializable
data class CreateWarehouseRequest(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class UpdateWarehouseRequest(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class WarehouseListItemResponse(
    val id: Int,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class WarehouseDetailsResponse(
    val id: Int,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val active: Boolean,
)
