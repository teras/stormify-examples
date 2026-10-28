package demo

import onl.ycode.stormify.generated.GeneratedEntities
import onl.ycode.stormify.Stormify
import org.sqlite.SQLiteDataSource
import java.io.File

fun main() {
    File("build/demo.db").delete()

    // The schema below declares REFERENCES, but SQLite ignores foreign keys unless
    // they are switched on per connection.
    val ds = SQLiteDataSource().apply {
        url = "jdbc:sqlite:build/demo.db"
        setEnforceForeignKeys(true)
    }
    val stormify = Stormify(ds, GeneratedEntities)

    runDemo(stormify)

    File("build/demo.db").delete()
}
