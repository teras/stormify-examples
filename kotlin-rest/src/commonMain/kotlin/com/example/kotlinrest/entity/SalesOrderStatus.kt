package com.example.kotlinrest.entity

import kotlinx.serialization.Serializable

@Serializable
enum class SalesOrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPED,
    CANCELLED,
}
