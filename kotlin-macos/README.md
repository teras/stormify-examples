# Stormify Kotlin/Native macOS Demo

A self-contained Kotlin/Native application demonstrating Stormify ORM on macOS without a JVM.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.1.1 https://github.com/teras/stormify-examples.git
cd stormify-examples/kotlin-macos
```

## Highlights

This example runs as a **native macOS binary** — both `macosArm64` (Apple Silicon) and `macosX64` (Intel) targets are built, sharing source code through the `macosMain` intermediate source set. No JVM required at runtime. It uses Stormify's native SQLite driver via KDBC. Entity metadata is generated at compile time by the KSP annotation processor (`annproc`), which is required on native platforms.

The `Task` entity extends `AutoTable` with `by db()` delegates for automatic lazy-loading. The `User` entity is a plain class — references to it must be populated explicitly via `stormify.populate()`.

## What it demonstrates

- **Native binary** — compiles to a standalone macOS executable
- **KSP annotation processor** — generates entity metadata at compile time
- **AutoTable vs plain class** — `Task` auto-populates references, `User` requires explicit `populate()`
- **CRUD operations** — create, findById, findAll, update, delete
- **Transaction DSL** with automatic rollback on exception
- **Raw SQL JOIN query** returning `Map<String, Any?>` results

## Requirements

macOS ships with `libsqlite3.dylib` — no extra installation needed.

## Build & Run

Release build (optimized, stripped):

On Apple Silicon:
```bash
gradle linkReleaseExecutableMacosArm64
./build/bin/macosArm64/releaseExecutable/stormify-kotlin-macos-demo.kexe
```

On Intel:
```bash
gradle linkReleaseExecutableMacosX64
./build/bin/macosX64/releaseExecutable/stormify-kotlin-macos-demo.kexe
```

Or build + run in one step (debug): `gradle runDebugExecutableMacosArm64` / `runDebugExecutableMacosX64`.
