# Kotlin Rest

A Ktor REST server backed by Stormify over SQLite, built as a Kotlin/Native binary. It
shows how Stormify fits a realistic web service — entity model, DTOs, services, routes,
schema bootstrap — and exposes `POST /search` endpoints that take Stormify `PageSpec`
payloads for paged, filtered and sorted queries.

The code is deliberately explicit. Where a shorter abstraction would have hidden the
Stormify call that does the work, the repetition was kept instead: this is meant to be
read, not reused wholesale.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.6.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/kotlin-rest
```

## Requirements

- A system Gradle ≥ 8 (no wrapper is committed here).
- `libsqlite3` available at runtime — the native driver links against the system library.

## Run

```bash
gradle runDebugExecutableLinuxX64
```

The server listens on `http://localhost:8080`. On first run it creates the schema and
seeds it, so a fresh clone needs nothing else. Delete `data/` to start over. Executable
entry points are also declared for macOS and Windows targets.

For a release binary:

```bash
gradle linkReleaseExecutableLinuxX64
./build/bin/linuxX64/releaseExecutable/stormify-kotlin-rest-demo.kexe
```

## Configuration

Every setting is read from the environment, with the defaults below.

| Variable            | Default             | Meaning                                     |
|---------------------|---------------------|---------------------------------------------|
| `WAREHOUSE_DB_PATH` | `data/warehouse.db` | SQLite file; parent directories are created |
| `HOST`              | `0.0.0.0`           | Bind address — see the note below           |
| `PORT`              | `8080`              | Listen port                                 |
| `LOG_LEVEL`         | `INFO`              | `DEBUG` also logs every SQL statement       |

`0.0.0.0` and the permissive CORS policy are demo conveniences: they let the companion
frontend reach the server from another machine without configuration. A deployed service
would bind a specific interface and name its front end explicitly.

## Endpoints

Every resource exposes the same six routes, where `<resource>` is one of `categories`,
`suppliers`, `customers`, `warehouses`, `products`, `stock-items`, `purchase-orders`,
`sales-orders`, `shipments`:

| Method   | Path                     | Answers                                  |
|----------|--------------------------|------------------------------------------|
| `POST`   | `/api/<resource>/search` | a `PagedResponse` page                   |
| `POST`   | `/api/<resource>/export` | CSV of the whole result set, streamed    |
| `POST`   | `/api/<resource>`        | `201` + `Location` of the new row        |
| `GET`    | `/api/<resource>/{id}`   | one row, or `404`                        |
| `PUT`    | `/api/<resource>/{id}`   | the updated row                          |
| `DELETE` | `/api/<resource>/{id}`   | `204`, or `404`, or `409` if referenced  |

Orders and shipments add their state transitions:
`POST /api/purchase-orders/{id}/receive`, `POST /api/sales-orders/{id}/confirm`,
`POST /api/shipments/{id}/ship`. `GET /api/health` answers `{"status":"ok"}` together with a
snapshot of the connection pool's counters (see *Pooling* below).

### Examples

Search — a page of products, sorted, filtered by a facet:

```bash
curl -X POST http://localhost:8080/api/products/search \
  -H 'Content-Type: application/json' \
  -d '{"page":0,"pageSize":10,"filters":{"supplierName":"Supplier 03"},"sorts":{"sku":"ASC"}}'
```

Create — answers `201` with a `Location` header:

```bash
curl -i -X POST http://localhost:8080/api/categories \
  -H 'Content-Type: application/json' \
  -d '{"name":"Fasteners","description":"Screws and bolts","active":true}'
```

Export — CSV, streamed:

```bash
curl -X POST http://localhost:8080/api/stock-items/export \
  -H 'Content-Type: application/json' -d '{"page":0,"pageSize":50}'
```

## Error contract

Every failure answers JSON of the shape
`{"message": …, "errorCode": …, "details": {…}}`:

| Status | `errorCode`              | When                                              |
|--------|--------------------------|---------------------------------------------------|
| `400`  | `VALIDATION_ERROR`       | a field is blank, negative, or missing            |
| `400`  | `BAD_REQUEST`            | the body is not the JSON expected                 |
| `404`  | `ENTITY_NOT_FOUND`       | the id in the **path** names nothing              |
| `409`  | `CONFLICT`               | the state refuses it, or a constraint rejected it |
| `413`  | `PAYLOAD_TOO_LARGE`      | the request body is larger than 1 MB              |
| `415`  | `UNSUPPORTED_MEDIA_TYPE` | the request was not `application/json`            |
| `422`  | `REFERENCE_NOT_FOUND`    | an id in the **body** names nothing               |

## Money and dates

Money is stored and exchanged as **integer cents** (`"unitPrice": 64536` is 645.36).
Formatting is the client's job. Dates are ISO-8601 strings of fixed precision, so they
sort lexicographically, and a date that has not happened yet is `null` rather than `""`.

## Pooling

Every request runs on Stormify's suspend API (`stormify.suspending`), which owns a real
connection pool — the only way to pool on Kotlin/Native, where each connection is pinned to
its own thread. Reads borrow a connection (`withConnection`); writes that must be atomic take
a transaction (`async.transaction`). `GET /api/health` reports the pool's live counters, and
the pool is closed on shutdown so the process does not leak the threads behind it.

## Tests

```bash
gradle linuxX64Test
```

The service-level suite in `src/commonTest` drives the same suspend services the routes call:
document-number sequencing, a rejected validation, a missing-id delete, the insufficient-stock
conflict, and the one-shipment-per-order rule. Each test seeds its own temporary database, so
the suite needs neither a running server nor an external database.

## Package layout

```text
src/commonMain/kotlin/com/example/kotlinrest/
  Main.kt        # settings → openDatabase → applySchemaIfNeeded → startServer
  config/        # AppSettings, read from the environment
  db/            # connection setup, schema DDL, seed data
  dto/           # request/response payloads
  entity/        # Stormify AutoTable entities
  exception/     # the typed error hierarchy
  server/        # ktor wiring, routes, request/response helpers
  service/       # business logic, one file per resource
  support/       # CSV, paging, document numbers, time
```

## Companion frontend

The sibling [`frontend-react`](../frontend-react) folder is a React + TypeScript admin UI
(Vite, MUI, AG Grid, TanStack Query, React Router) that consumes this server through the
same `PageSpec`/`PagedResponse` contract. Start the backend as above, then:

```bash
cd ../frontend-react
npm install
npm run dev
```

The backend URL is configurable at runtime from the sidebar, so one frontend build can
switch between servers without rebuilding.
