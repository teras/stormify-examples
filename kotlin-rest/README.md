# Kotlin Rest

A Ktor REST server backed by Stormify over SQLite, built with Kotlin Multiplatform. It demonstrates how Stormify fits into a realistic web-service layout — entity model, DTOs, service layer, routes, seed data — and exposes `POST /search` endpoints that take Stormify `PageSpec` payloads for paged/filtered/sorted queries.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.5.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/kotlin-rest
```

## Run

```bash
gradle run
```

The server starts on `http://localhost:8080`, seeds an SQLite database on first run, and exposes paged/filtered/sorted `POST /search` endpoints per entity. Native executable entry points are also declared for Linux, macOS, and Windows targets.

## Stack

- Kotlin Multiplatform
- Ktor for the REST layer
- Stormify for ORM
- SQLite for persistence

## Package layout

```text
src/commonMain/kotlin/com/example/kotlinrest/
  app/         # application entry and server wiring
  config/      # configuration loading
  dto/         # request/response payloads
  entity/      # Stormify AutoTable entities
  exception/   # typed error handling
  route/       # Ktor route definitions
  seed/        # initial data population
  service/     # business logic layer
```

Entities are modeled as Stormify `AutoTable` classes. The code is intentionally explicit and heavily structured because this project is designed as a readable demo.

## Companion frontend

The sibling [`frontend-react`](../frontend-react) folder in this repository is a React + TypeScript admin UI (Vite, MUI Data Grid, TanStack Query, React Router) that consumes this REST server through the same `PageSpec`/`PagedResponse` contract. It is the visible half of the paged-search endpoints served here — on its own this folder is just a Ktor backend, but together with `frontend-react` the pair shows a full client/server walkthrough of how Stormify's paging model reaches an end user.

To run both together, start the backend as shown above, then in a separate terminal:

```bash
cd ../frontend-react
npm install
npm run dev
```

The backend URL is configurable at runtime from the sidebar, so the same frontend build can switch between servers without rebuilding.
