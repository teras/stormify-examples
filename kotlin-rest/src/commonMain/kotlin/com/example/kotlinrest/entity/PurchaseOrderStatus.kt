package com.example.kotlinrest.entity

import kotlinx.serialization.Serializable

@Serializable
enum class PurchaseOrderStatus {
    DRAFT,
    RECEIVED,
}
