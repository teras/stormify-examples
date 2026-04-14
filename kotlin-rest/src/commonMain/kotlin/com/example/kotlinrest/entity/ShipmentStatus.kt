package com.example.kotlinrest.entity

import kotlinx.serialization.Serializable

@Serializable
enum class ShipmentStatus {
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}
