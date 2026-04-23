# Stormify Kotlin JVM Demo

A self-contained Kotlin JVM application demonstrating Stormify ORM with an SQLite database.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.5.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/kotlin-jvm
```

## Highlights

Both entities extend `AutoTable` with `by db()` property delegates for **automatic lazy-loading**. The `Task.user` field is a reference to a `User` entity — Stormify resolves it from the `user_id` foreign key column, and its data is loaded transparently on first property access.

Compared to the Java example, notice how Kotlin's `by db()` delegates replace manual `populate()` calls, and the DSL-style transactions make the code more concise.

## What it demonstrates

- **Schema setup** with raw SQL (`executeUpdate`)
- **CRUD operations** — create, findById, findAll, update, delete
- **Entity references** with lazy loading (`AutoTable` + `by db()`)
- **Transaction DSL** with `stormify.transaction { ... }`
- **Automatic rollback** on exception
- **Raw SQL JOIN query** returning `Map<String, Any?>` results

## Run

```bash
gradle run
```
