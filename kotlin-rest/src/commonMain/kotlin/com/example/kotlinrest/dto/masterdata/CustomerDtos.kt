package com.example.kotlinrest.dto.masterdata

import com.example.kotlinrest.entity.CustomerType
import kotlinx.serialization.Serializable

@Serializable
data class CreateCustomerRequest(
    val name: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val customerType: CustomerType,
    val active: Boolean,
)

@Serializable
data class UpdateCustomerRequest(
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
