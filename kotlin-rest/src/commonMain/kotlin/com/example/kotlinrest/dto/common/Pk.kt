package com.example.kotlinrest.dto.common

/**
 * The primary key a persisted entity is guaranteed to carry. Response mappers read it after a
 * row has been created or loaded, so a null here means the row was never saved — a bug, not a
 * value to paper over with `0`. Failing loudly points at the real mistake.
 */
fun pk(id: Int?): Int = checkNotNull(id) { "entity read from the database has a null id" }
