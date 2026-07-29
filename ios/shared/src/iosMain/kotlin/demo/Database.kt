package demo

import onl.ycode.stormify.generated.stormifyEntities
import onl.ycode.kdbc.KdbcDataSource
import onl.ycode.stormify.*
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

/**
 * Single owner of the Stormify instance for the iOS demo app.
 *
 * Same pattern as the Android example: one Stormify instance for the lifetime
 * of the process, schema bootstrap on first launch, seed data if empty.
 */
object Database {
    @kotlin.concurrent.Volatile
    private var instance: Stormify? = null

    /**
     * Opens the database. Call this once, from app start, before any other function
     * here. (Named `prepare` rather than `initialize` because Kotlin/Native renames
     * anything starting with `init` when exporting to Objective-C.)
     *
     * A lazy singleton would be the shorter spelling and the wrong one: the app calls
     * into these functions from background tasks, so two of them could reach a lazy
     * initializer at the same time and each build its own instance. Initialising
     * explicitly at a moment when only one thread is running removes the race instead
     * of trying to guard it, and it makes the point of the lifecycle visible.
     */
    fun prepare() {
        if (instance == null) instance = createInstance()
    }

    fun open(): Stormify = instance
        ?: error("Database.prepare() must be called at app start before any database call")

    private fun createInstance(): Stormify {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).first() as String
        val dbPath = "$docs/stormify-demo.db"

        // SQLite disables foreign keys per connection by default, so the schema's
        // ON DELETE CASCADE would never fire without this.
        val ds = KdbcDataSource("jdbc:sqlite:$dbPath", initSql = "PRAGMA foreign_keys = ON")
        // `stormifyEntities` rather than `GeneratedEntities`: the processor emits the
        // latter only into leaf source sets, and this file is shared by the device and
        // simulator targets. The shim is the handle intended for shared code.
        val stormify = Stormify(ds, stormifyEntities).asDefault()

        bootstrapSchema(stormify)
        seedIfEmpty(stormify)
        return stormify
    }

    private fun bootstrapSchema(stormify: Stormify) {
        stormify.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS user (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL
            )
            """.trimIndent()
        )
        stormify.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS task (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                is_completed INTEGER NOT NULL DEFAULT 0,
                priority INTEGER NOT NULL DEFAULT 20,
                user_id INTEGER NOT NULL REFERENCES user(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun seedIfEmpty(stormify: Stormify) {
        val userCount = stormify.readOne<Long>("SELECT COUNT(*) FROM user") ?: 0L
        if (userCount > 0) return

        stormify.transaction {
            val alice = User(name = "Alice", email = "alice@example.com").create()
            val bob = User(name = "Bob", email = "bob@example.com").create()

            Task().apply {
                title = "Wire up Stormify"
                description = "Drop the library into the app and run it."
                priority = Priority.HIGH
                user = alice
            }.create()
            Task().apply {
                title = "Show off lazy refs"
                description = "Tap a row \u2014 the user is loaded on demand."
                priority = Priority.MEDIUM
                user = alice
            }.create()
            Task().apply {
                title = "Review the example"
                description = "Skim the code; it should read top-to-bottom."
                priority = Priority.LOW
                user = bob
            }.create()
        }
    }
}

// === Swift-callable API ===

fun getAllTasks(): List<Task> {
    val s = Database.open()
    return findAll<Task>().onEach { it.user?.let { u -> s.refresh(u) } }
}

fun getAllUsers(): List<User> {
    Database.open()
    return findAll()
}

fun addTask(title: String, description: String, priority: Priority, owner: User) {
    Database.open()
    // A single insert is already atomic; there is no second write that has to land
    // with it, so there is no transaction to open.
    Task().apply {
        this.title = title
        this.description = description
        this.priority = priority
        this.user = owner
    }.create()
}

fun toggleCompleted(task: Task) {
    Database.open()
    task.isCompleted = !task.isCompleted
    task.update()
}

fun deleteTask(task: Task) {
    Database.open()
    task.delete()
}
