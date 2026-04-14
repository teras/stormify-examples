package com.example.kotlinrest

import com.example.kotlinrest.config.DatabaseFactory
import com.example.kotlinrest.config.DatabaseSettings
import com.example.kotlinrest.server.startServer

fun main() {
    val settings = DatabaseSettings()
    DatabaseFactory(settings).createStormify()
    startServer(settings)
}
