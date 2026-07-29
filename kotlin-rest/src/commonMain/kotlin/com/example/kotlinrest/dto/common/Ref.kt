package com.example.kotlinrest.dto.common

import kotlinx.serialization.Serializable

/**
 * A minimal `{id, label}` pointer to a related row, embedded in a details response so the
 * frontend can show and link it without a second request. One shape serves every relation —
 * a product's category, an order's warehouse — so the client has a single type to handle.
 */
@Serializable
data class Ref(
    val id: Int,
    val label: String,
)

/**
 * Builds a [Ref] only when both parts are present, so an absent relation serializes as `null`
 * rather than a `{0, ""}` placeholder the frontend would have to special-case.
 */
fun ref(id: Int?, label: String?): Ref? =
    if (id == null || label == null) null else Ref(id, label)
