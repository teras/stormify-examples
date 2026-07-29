# Stormify Java Demo

A self-contained Java application demonstrating Stormify ORM with an SQLite database.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.6.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/java-pom
```

## Highlights

This example showcases **mix-and-match annotations**: the `User` entity uses standard **JPA annotations** (`@Id`, `@GeneratedValue`), while the `Task` entity uses **Stormify annotations** (`@DbTable`, `@DbField`). Both work seamlessly in the same project.

`Task` extends `AutoTable` for **lazy-loaded references** — `Task.user` is resolved from the `user_id` foreign key column and loaded on first access via `hydrate()`. `User` is deliberately a plain class, so its references must be filled in explicitly with `stormify.refresh()`.

## What it demonstrates

- **Schema setup** with raw SQL (`executeUpdate`)
- **CRUD operations** — create, findById, findAll, update, delete
- **AutoTable vs plain class** — `Task` auto-hydrates references, `User` requires explicit `refresh()`
- **Transactions** with automatic rollback on exception
- **Raw SQL JOIN query** returning `Map<String, Object>` results
- **JPA + Stormify annotation** interoperability

## Run

```bash
mvn compile exec:java
```
