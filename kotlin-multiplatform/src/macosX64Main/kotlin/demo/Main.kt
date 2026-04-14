package demo

import onl.ycode.stormify.generated.GeneratedEntities
import onl.ycode.kdbc.KdbcDataSource
import onl.ycode.stormify.Stormify
import platform.posix.remove

fun main() {
    remove("build/demo.db")

    val ds = KdbcDataSource("jdbc:sqlite:build/demo.db")
    val stormify = Stormify(ds, GeneratedEntities)

    runDemo(stormify)

    remove("build/demo.db")
}
