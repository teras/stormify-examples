package com.example.kotlinrest.config

import onl.ycode.kdbc.Connection
import onl.ycode.kdbc.DataSource
import onl.ycode.kdbc.KdbcDataSource
import onl.ycode.kdbc.Statement
import onl.ycode.logger.LogLevel
import onl.ycode.stormify.*
import onl.ycode.stormify.generated.stormifyEntities

class DatabaseFactory(
    private val settings: DatabaseSettings,
) {
    fun createStormify(): Stormify {
        val raw = KdbcDataSource("jdbc:sqlite:${settings.databasePath}")
        val dataSource = object : DataSource {
            override fun getConnection(): Connection {
                val conn = raw.getConnection()
                conn.initStatement("PRAGMA foreign_keys = ON", false, null).executeUpdate()
                return conn
            }
        }
        return Stormify(dataSource, stormifyEntities).apply {
            logger.level = LogLevel.DEBUG
        }.asDefault()
    }
}
