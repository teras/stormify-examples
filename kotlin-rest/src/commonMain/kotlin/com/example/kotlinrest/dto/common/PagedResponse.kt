package com.example.kotlinrest.dto.common

import kotlinx.serialization.Serializable

/**
 * The API keeps a stable transport envelope even though the query execution
 * underneath is powered by Stormify's stateless PagedQuery API.
 */
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)
