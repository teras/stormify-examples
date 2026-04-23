# Stormify Java + Gradle Demo

A Java application built with Gradle, demonstrating Stormify ORM with type-safe
generated paths via the Stormify Gradle plugin.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.5.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/java-gradle
```

## Highlights

This is the [`java`](../java) example ported to Gradle, with one extra feature the
Maven version can't offer: **type-safe generated paths** (`Tables.Task_.user.name`)
produced automatically by the Stormify Gradle plugin at compile time. Paths are
refactor-safe — renaming or deleting a property stops the project from compiling.

The Kotlin Gradle plugin is applied solely so the Stormify plugin's KSP runs;
all source code remains pure Java.

## What it demonstrates

- **Schema setup** with raw SQL (`executeUpdate`)
- **CRUD operations** — create, findById, findAll, update, delete
- **Entity references** with lazy loading (`AutoTable` + `populate()`)
- **Transactions** with automatic rollback on exception
- **Type-safe paths** from the plugin-generated `Tables` object
- **Raw SQL JOIN query** returning `Map<String, Object>` results
- **JPA + Stormify annotation** interoperability

## Run

```bash
gradle run
```
