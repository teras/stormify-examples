# Stormify Kotlin/Native Windows Demo

A self-contained Kotlin/Native application demonstrating Stormify ORM on Windows without a JVM.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 3.0.1 https://github.com/teras/stormify-examples.git
cd stormify-examples/kotlin-windows
```

## Highlights

This example runs as a **native Windows binary** (`mingwX64`) — no JVM required at runtime. It uses Stormify's native SQLite driver via KDBC. Entity metadata is generated at compile time by the KSP annotation processor (`annproc`), which is required on native platforms.

The `Task` entity extends `AutoTable` with `by db()` delegates for automatic lazy-loading. The `User` entity is a plain class — references to it must be populated explicitly via `stormify.refresh()`.

## What it demonstrates

- **Native binary** — compiles to a standalone Windows executable
- **KSP annotation processor** — generates entity metadata at compile time
- **AutoTable vs plain class** — `Task` auto-hydrates references, `User` requires explicit `refresh()`
- **CRUD operations** — create, findById, findAll, update, delete
- **Transaction DSL** with automatic rollback on exception
- **Raw SQL JOIN query** returning `Map<String, Any?>` results

## Requirements

The native SQLite driver loads `sqlite3.dll` at runtime. Download the 64-bit DLL
from [sqlite.org](https://www.sqlite.org/download.html) (*Precompiled Binaries for
Windows*) and place it next to the executable or on `PATH`.

## Build & Run

On Windows, build and run in one step (debug):

```bash
gradle runDebugExecutableMingwX64
```

On a Linux host Gradle builds the `.exe` but skips running it. Link a release build
(optimized, stripped), put `sqlite3.dll` next to it and run it under Wine. Run it from
this folder: the demo creates its database under `build/`.

```bash
gradle linkReleaseExecutableMingwX64
cp /path/to/sqlite3.dll build/bin/mingwX64/releaseExecutable/
wine build/bin/mingwX64/releaseExecutable/stormify-kotlin-windows-demo.exe
```

The `.exe` lives at `build/bin/mingwX64/releaseExecutable/stormify-kotlin-windows-demo.exe`.
