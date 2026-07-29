package com.example.kotlinrest.dto.masterdata

import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.entity.Supplier
import kotlinx.serialization.Serializable

@Serializable
data class SupplierRequest(
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

fun Supplier.toListItemResponse() = SupplierListItemResponse(
    id = pk(id),
    name = name,
    contactName = contactName,
    city = city,
    country = country,
    active = active,
)

fun Supplier.toDetailsResponse() = SupplierDetailsResponse(
    id = pk(id),
    name = name,
    contactName = contactName,
    email = email,
    phone = phone,
    city = city,
    country = country,
    active = active,
)
