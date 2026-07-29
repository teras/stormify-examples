package demo

import onl.ycode.stormify.generated.GeneratedEntities
import onl.ycode.kdbc.KdbcDataSource
import onl.ycode.stormify.Stormify
import platform.posix.remove

fun main() {
    remove("build/demo.db")

    // The schema below declares REFERENCES, but SQLite ignores foreign keys unless
    // they are switched on per connection.
    val ds = KdbcDataSource("jdbc:sqlite:build/demo.db", initSql = "PRAGMA foreign_keys = ON")
    val stormify = Stormify(ds, GeneratedEntities)

    runDemo(stormify)

    remove("build/demo.db")
}
