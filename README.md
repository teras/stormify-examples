# Stormify Examples

Ready-to-run sample projects for [Stormify](https://github.com/teras/stormify), the flexible Kotlin Multiplatform ORM. Every example uses SQLite as the default database (no server setup required) and is self-contained.

## Getting the examples

Clone the repository and step into the folder you are interested in:

```bash
git clone -b 2.5.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/<example-folder>
```

Each subfolder has its own `README.md` with platform-specific notes and prerequisites.

## Available examples

| Example | What it demonstrates | Run |
| --- | --- | --- |
| [kotlin-jvm](kotlin-jvm) | Shortest path to a working Stormify app on the JVM, with `AutoTable` + `by db()` delegates. | `gradle run` |
| [kotlin-linux](kotlin-linux) | Standalone Kotlin/Native Linux binary (`linuxX64`) — no JVM at runtime, native SQLite via KDBC. | `gradle runDebugExecutableLinuxX64` |
| [kotlin-windows](kotlin-windows) | Standalone Kotlin/Native Windows binary (`mingwX64`). Runs natively on Windows or under Wine. | `gradle runDebugExecutableMingwX64` |
| [kotlin-macos](kotlin-macos) | Standalone Kotlin/Native macOS binary for `macosArm64` + `macosX64`. Requires macOS to build. | `gradle runDebugExecutableMacosArm64` |
| [kotlin-multiplatform](kotlin-multiplatform) | Same business logic in `commonMain` running on JVM + every supported native target. | `gradle run` / `gradle runDebugExecutable<Target>` |
| [java-pom](java-pom) | Same scenario in Java, mixing JPA (`@Id`, `@GeneratedValue`) and Stormify (`@DbTable`, `@DbField`) annotations in one project. Maven, reflection-based. | `mvn compile exec:java` |
| [java-gradle](java-gradle) | Same scenario in Java, built with Gradle + the Stormify plugin — adds compile-time type-safe paths (`Tables.Task_.user.name`). | `gradle run` |
| [android](android) | Compose app using Android's built-in SQLite via the `Stormify(SQLiteDatabase)` constructor; min SDK 21. | `gradle :app:installDebug` |
| [ios](ios) | SwiftUI app backed by a Kotlin Multiplatform shared module; platform SQLite via cinterop. Requires macOS + Xcode. | Open `ios/iosApp/iosApp.xcodeproj` in Xcode |
| [kotlin-rest](kotlin-rest) | Ktor REST server exposing `POST /search` endpoints built on Stormify's `PageSpec`/`PagedResponse` contract. | `gradle run` |
| [frontend-react](frontend-react) | React + TypeScript admin UI that consumes [`kotlin-rest`](kotlin-rest) through the same paging contract. | `npm install && npm run dev` |

## Where to start

- Want the shortest path to a working Stormify app? → [`kotlin-jvm`](kotlin-jvm), [`java-gradle`](java-gradle) (Gradle + type-safe paths), or [`java-pom`](java-pom) (Maven, reflection-based).
- Want to share persistence code across every target Stormify supports? → [`kotlin-multiplatform`](kotlin-multiplatform).
- Want to see Stormify inside a realistic service layout, end to end? → [`kotlin-rest`](kotlin-rest) + [`frontend-react`](frontend-react).

## Documentation

- Project site: [stormify.org](https://stormify.org/)
- Docs: [stormify.org/docs/](https://stormify.org/docs/)
- Main Stormify repository: [teras/stormify](https://github.com/teras/stormify)
