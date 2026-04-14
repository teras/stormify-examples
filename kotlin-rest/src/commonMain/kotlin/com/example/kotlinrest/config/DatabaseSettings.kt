package com.example.kotlinrest.config

/**
 * The database path stays externalized from the entity model so it is clear
 * which values belong to infrastructure and which belong to domain.
 */
data class DatabaseSettings(
    val databasePath: String = "data/warehouse.db",
    val bootstrapMarkerTable: String = "category",
    val host: String = "0.0.0.0",
    val port: Int = 8080,
)
