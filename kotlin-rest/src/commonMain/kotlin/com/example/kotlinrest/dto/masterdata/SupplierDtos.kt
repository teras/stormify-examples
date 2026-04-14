package com.example.kotlinrest.dto.masterdata

import kotlinx.serialization.Serializable

@Serializable
data class CreateSupplierRequest(
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class UpdateSupplierRequest(
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class SupplierListItemResponse(
    val id: Int,
    val name: String,
    val contactName: String,
    val city: String,
    val country: String,
    val active: Boolean,
)

@Serializable
data class SupplierDetailsResponse(
    val id: Int,
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val active: Boolean,
)
