package demo;

import onl.ycode.stormify.StormifyJ;
import onl.ycode.stormify.generated.GeneratedEntities;
import onl.ycode.stormify.generated.Tables;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        // Clean up any previous run
        new File("build/demo.db").delete();

        // Create a SQLite DataSource
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:build/demo.db");

        // Initialize Stormify with the plugin-generated registrar. Pass
        // GeneratedEntities so no reflection is needed at runtime.
        final StormifyJ stormify = new StormifyJ(ds, GeneratedEntities.INSTANCE).asDefault();

        // === Schema Setup (Low-Level SQL API) ===
        System.out.println("=== Schema Setup ===");
        stormify.executeUpdate(
                "CREATE TABLE user (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "email TEXT NOT NULL)"
        );
        stormify.executeUpdate(
                "CREATE TABLE task (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "title TEXT NOT NULL, " +
                        "description TEXT, " +
                        "is_completed INTEGER NOT NULL DEFAULT 0, " +
                        "priority INTEGER, " +
                        "user_id INTEGER NOT NULL REFERENCES user(id))"
        );
        System.out.println("Tables created.\n");

        // === Create (ORM) ===
        System.out.println("=== Creating Users ===");
        stormify.transaction(() -> {
            User alice = new User();
            alice.setName("Alice");
            alice.setEmail("alice@example.com");
            stormify.create(alice);
            System.out.println("Created: " + alice);

            User bob = new User();
            bob.setName("Bob");
            bob.setEmail("bob@example.com");
            stormify.create(bob);
            System.out.println("Created: " + bob);

            System.out.println("\n=== Creating Tasks ===");
            Task t1 = new Task(0, "Set up database", "Configure schema and indexes", false, Priority.HIGH, alice);
            stormify.create(t1);
            System.out.println("Created: " + t1);

            Task t2 = new Task(0, "Write documentation", "API reference and examples", false, Priority.MEDIUM, alice);
            stormify.create(t2);
            System.out.println("Created: " + t2);

            Task t3 = new Task(0, "Review pull request", "Check code style and tests", false, Priority.LOW, bob);
            stormify.create(t3);
            System.out.println("Created: " + t3);
        });

        // === Read (ORM) ===
        System.out.println("\n=== Find by ID ===");
        User foundUser = stormify.findById(User.class, 1);
        System.out.println("Found user: " + foundUser);

        Task foundTask = stormify.findById(Task.class, 2);
        System.out.println("Found task: " + foundTask);

        // === Reference loading demo ===
        System.out.println("\n=== Reference Loading ===");
        User userRef = foundTask.getUser();
        System.out.println("Before populate: " + userRef);
        stormify.populate(userRef);
        System.out.println("After populate:  " + userRef);

        System.out.println("\n=== Find All ===");
        List<Task> allTasks = stormify.findAll(Task.class);
        allTasks.forEach(t -> System.out.println("  " + t));

        // === Type-safe Paths (plugin-generated) ===
        // The Stormify Gradle plugin emits a `Tables` holder under
        // `onl.ycode.stormify.generated` containing one entry per entity. Each
        // entry walks the foreign keys and exposes every scalar as a typed
        // path. No strings, no magic — refactor-safe.
        System.out.println("\n=== Type-safe Paths ===");
        System.out.println("Path to Task.title:         " + Tables.Task_.title);
        System.out.println("Path to Task.user.name:     " + Tables.Task_.user().name);
        System.out.println("Path to Task.user.email:    " + Tables.Task_.user().email);

        // === Update (ORM) ===
        System.out.println("\n=== Update Task ===");
        foundTask.setCompleted(true);
        stormify.update(foundTask);
        Task updated = stormify.findById(Task.class, foundTask.getId());
        System.out.println("Updated: " + updated);

        // === Delete (ORM) ===
        System.out.println("\n=== Delete Task ===");
        Task toDelete = stormify.findById(Task.class, 3);
        System.out.println("Deleting: " + toDelete);
        stormify.delete(toDelete);
        List<Task> remaining = stormify.findAll(Task.class);
        System.out.println("Remaining tasks: " + remaining.size());

        // === Transaction Rollback ===
        System.out.println("\n=== Transaction Rollback Demo ===");
        int countBefore = stormify.findAll(Task.class).size();
        try {
            stormify.transaction(() -> {
                User u = stormify.findById(User.class, 1);
                stormify.create(new Task(0, "Temporary task", "...", false, Priority.LOW, u));
                System.out.println("Task created inside transaction");
                throw new RuntimeException("Something went wrong!");
            });
        } catch (RuntimeException e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }
        int countAfter = stormify.findAll(Task.class).size();
        System.out.println("Tasks before: " + countBefore + ", after: " + countAfter + " (unchanged)");

        // === Raw SQL with JOIN (Low-Level API) ===
        System.out.println("\n=== Raw SQL JOIN Query ===");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) stormify.read(
                Map.class,
                "SELECT u.name AS user_name, t.title AS task_title, t.is_completed " +
                        "FROM task t JOIN user u ON t.user_id = u.id"
        );
        results.forEach(row -> System.out.println("  " + row));

        System.out.println("\nDone!");
        new File("build/demo.db").delete();
    }
}
