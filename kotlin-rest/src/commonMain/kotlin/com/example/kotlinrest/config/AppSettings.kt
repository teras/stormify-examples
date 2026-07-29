package com.example.kotlinrest.config

import com.example.kotlinrest.db.envOrNull
import onl.ycode.logger.LogLevel

data class AppSettings(
    val dbPath: String = envOrNull("WAREHOUSE_DB_PATH") ?: "data/warehouse.db",
    val host: String = envOrNull("HOST") ?: "0.0.0.0",
    val port: Int = envOrNull("PORT")?.toIntOrNull() ?: 8080,
    val logLevel: LogLevel = envOrNull("LOG_LEVEL")
        ?.let { runCatching { LogLevel.valueOf(it.uppercase()) }.getOrNull() }
        ?: LogLevel.INFO,
    val prettyPrint: Boolean = false,
)
