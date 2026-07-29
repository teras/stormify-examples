package com.example.kotlinrest.dto.masterdata

import com.example.kotlinrest.dto.common.pk
import com.example.kotlinrest.entity.Customer
import com.example.kotlinrest.entity.CustomerType
import kotlinx.serialization.Serializable

@Serializable
data class CustomerRequest(
    val name: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val customerType: CustomerType,
    val active: Boolean,
)

@Serializable
data class CustomerListItemResponse(
    val id: Int,
    val name: String,
    val email: String,
    val city: String,
    val country: String,
    val customerType: CustomerType,
    val active: Boolean,
)

@Serializable
data class CustomerDetailsResponse(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val customerType: CustomerType,
    val active: Boolean,
)

fun Customer.toListItemResponse() = CustomerListItemResponse(
    id = pk(id),
    name = name,
    email = email,
    city = city,
    country = country,
    customerType = customerType,
    active = active,
)

fun Customer.toDetailsResponse() = CustomerDetailsResponse(
    id = pk(id),
    name = name,
    email = email,
    phone = phone,
    city = city,
    country = country,
    customerType = customerType,
    active = active,
)
