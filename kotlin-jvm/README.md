# Stormify Kotlin JVM Demo

A self-contained Kotlin JVM application demonstrating Stormify ORM with an SQLite database.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.6.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/kotlin-jvm
```

## Highlights

The `Task` entity extends `AutoTable` with `by db()` property delegates for **automatic lazy-loading**: `Task.user` is resolved from the `user_id` foreign key column and its data arrives transparently on first property access. `User` is deliberately a **plain class** — references to it must be filled in explicitly with `stormify.refresh()`. The contrast between the two is the point of this example.

Compared to the Java example, notice how Kotlin's `by db()` delegates replace manual `hydrate()` calls, and the DSL-style transactions make the code more concise.

## What it demonstrates

- **Schema setup** with raw SQL (`executeUpdate`)
- **CRUD operations** — create, findById, findAll, update, delete
- **AutoTable vs plain class** — `Task` auto-hydrates references, `User` requires explicit `refresh()`
- **Transaction DSL** with `stormify.transaction { ... }`
- **Automatic rollback** on exception
- **Raw SQL JOIN query** returning `Map<String, Any?>` results

## Run

```bash
gradle run
```
