# frontend-react

React + TypeScript admin client for the Stormify REST demo. It is the **visible half** of the [`kotlin-rest`](../kotlin-rest) example — the Ktor server exposes paged/filtered/sorted search endpoints built around Stormify's `PageSpec`/`PagedResponse` contract, and this UI is what consumes them. Looked at on its own, this folder is just a React app; looked at together with `kotlin-rest`, the pair is a full client/server walkthrough of how Stormify's paging model reaches an end user.

## Getting this example

The examples live in a single GitHub repo. Clone it and step into this folder:

```bash
git clone -b 2.5.0 https://github.com/teras/stormify-examples.git
cd stormify-examples/frontend-react
```

## Running it together with the backend

This frontend needs a running backend to talk to. Start [`kotlin-rest`](../kotlin-rest) first:

```bash
cd ../kotlin-rest
gradle run
```

Then, in a separate terminal:

```bash
cd frontend-react
npm install
npm run dev
```

## Notes

- Search/list pages use the backend `POST /search` endpoints.
- Those endpoints expect Stormify `PageSpec` payloads.
- The frontend keeps its own stable `PagedResponse` handling on the response side.
