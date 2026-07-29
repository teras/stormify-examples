package com.example.kotlinrest

import com.example.kotlinrest.config.AppSettings
import com.example.kotlinrest.db.applySchemaIfNeeded
import com.example.kotlinrest.db.openDatabase
import com.example.kotlinrest.server.startServer

fun main() {
    val settings = AppSettings()
    val stormify = openDatabase(settings)
    applySchemaIfNeeded(stormify)
    startServer(settings, stormify)
}
