package demo

import onl.ycode.stormify.generated.GeneratedEntities
import onl.ycode.stormify.Stormify
import org.sqlite.SQLiteDataSource
import java.io.File

fun main() {
    File("build/demo.db").delete()

    val ds = SQLiteDataSource().apply { url = "jdbc:sqlite:build/demo.db" }
    val stormify = Stormify(ds, GeneratedEntities)

    runDemo(stormify)

    File("build/demo.db").delete()
}
