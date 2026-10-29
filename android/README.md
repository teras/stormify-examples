# Stormify Android Example

A minimal Android app that demonstrates how to use **Stormify** with the
platform's built-in SQLite database. The whole sample is two entities, one
ViewModel, and one Compose screen — small enough to read in five minutes.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 3.0.1 https://github.com/teras/stormify-examples.git
cd stormify-examples/android
```

## What it shows

- Wiring `Stormify` to Android's `SQLiteDatabase` through KDBC's
  `AndroidDataSource`: `Stormify(AndroidDataSource(db), GeneratedEntities)`.
- The Stormify Gradle plugin running the KSP `annproc` processor at compile
  time, which generates the entity metadata registrar `GeneratedEntities` and
  the type-safe `Tables` paths. The registrar is registered when the `Stormify`
  instance is constructed, so entities are mapped without reflection.
- CRUD operations through the Stormify ORM API: `create`, `findAll`,
  `findById`, `update`, `delete`. Tapping a row toggles the task's completion,
  the bin icon deletes it, and the + button adds a new one. The first launch
  seeds the database inside one `transaction { }`.
- References via `AutoTable`: a `Task` resolves its `User`, which the ViewModel
  refreshes before the list is shown, so each row names its owner.
- Enum-as-int storage via `DbValue` so enum order changes don't corrupt data.

## Build & run

Build the app:

```bash
cd android
gradle :app:assembleDebug
```

To install on a connected emulator or device:

```bash
gradle :app:installDebug
adb shell am start -n demo.android/.MainActivity
```

## Notes

- `minSdk = 26`, `compileSdk = 34` — same floor as the Stormify Android library.
- Gradle finds the Android SDK through `ANDROID_HOME`, or through a
  `local.properties` file you create yourself (`sdk.dir=/path/to/Android/Sdk`).
- The database file is created at
  `/data/data/demo.android/databases/stormify-demo.db`. Uninstall the app to
  start from a clean seed.
