# Stormify iOS Example

A minimal iOS app that demonstrates how to use **Stormify** on iOS with SQLite
via Kotlin Multiplatform. The Kotlin shared module contains the entities and
demo logic; the Swift UI layer calls into it through the generated framework.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.1.1 https://github.com/teras/stormify-examples.git
cd stormify-examples/ios
```

## What it shows

- Using `KdbcDataSource` with SQLite on iOS (platform SQLite via C interop).
- The KSP `annproc` processor generating entity metadata at compile time
  (mandatory on native — reflection is not available).
- CRUD operations, transactions with rollback, and entity references.
- Kotlin/Native framework consumed by a SwiftUI app.

## Build & run

Open the Xcode project and run on a simulator:

```bash
cd iosApp
open iosApp.xcodeproj
```

## Notes

- Requires macOS with Xcode installed.
- Targets iOS 16+ (iosArm64 for device, iosSimulatorArm64 for simulator).
- The SQLite database is created in the app's temporary directory.
