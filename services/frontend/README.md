# frontend

React SPA for the incident management platform. Served by nginx on port **3000**; all `/api/` requests are proxied to `gateway:8080`.

## Stack

- **React 18** + **TypeScript** + **Vite 5**
- **Tailwind CSS** + **shadcn/ui** (Radix UI primitives, new-york style, zinc base)
- **TanStack Query v5** for data fetching and cache management
- **openapi-fetch** for type-safe HTTP calls against the generated schema
- **react-router-dom v6** for client-side routing

## Pages

| Route            | Page                                                                  |
| ---------------- | --------------------------------------------------------------------- |
| `/incidents`     | Incident list with search, status/severity filters, and create dialog |
| `/incidents/:id` | Incident detail — timeline, comments, AI insights tabs                |
| `/login`         | Login form (wired to `/api/v1/auth/login`)                            |

## OpenAPI integration

`services/generated/typescript/schema.d.ts` is generated from `api/openapi.yaml` by `api/scripts/gen-all.sh` (import as `@openapi/schema`), never edit it by hand.

To regenerate after changing the spec:

```bash
pixi run gen-all   # from the repo root
```

## Mock modes

There are two independent ways to run the frontend without the real backend:

### 1. In-file mock (`VITE_MOCK=true`)

Short-circuits all API queries to return hardcoded data from `src/api/queries.ts`. No backend or network required.

| Context                    | How mock mode is set                                                        |
| -------------------------- | --------------------------------------------------------------------------- |
| Local dev (`pixi run dev`) | `.env.development` sets `VITE_MOCK=true`                                    |
| Docker image               | `ARG VITE_MOCK=true` in `Dockerfile` (default, override with `--build-arg`) |
| Production / compose       | Set `VITE_MOCK=false`                                                       |

The flag is baked into the bundle at build time — it cannot be changed at runtime.

### 2. Prism mock server (`pixi run dev-mock-api`)

Runs the app against the Stoplight Prism mock server (`pixi run mock-api` from the repo root), which serves spec-driven responses from `api/openapi.yaml` on port **4010**. Unlike the in-file mock, this exercises the real `openapi-fetch` + TanStack Query stack over HTTP and stays in sync with the spec.

`VITE_MOCK` is left off in this mode (so queries make real network calls); `.env.mock-api` points `VITE_API_URL` at the Prism server. Prism runs with `--cors`, so the browser can call it cross-origin.

```bash
pixi run mock-api                  # repo root: Prism on :4010
# in a second terminal:
cd services/frontend
pixi run dev-mock-api              # dev server on :3000 -> :4010
```

## Local dev

```bash
cd services/frontend
pixi run install   # install node_modules (once)
pixi run dev       # dev server on :3000, hot reload, mock mode on
```

To verify the production bundle locally (`VITE_MOCK=false`):

```bash
pixi run prod   # builds dist/ then serves it on :3000, no hot reload
```
