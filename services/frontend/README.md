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

`src/api/schema.d.ts` is generated from `api/openapi.yaml` by `api/scripts/gen-all.sh`, never edit it by hand.

To regenerate after changing the spec:

```bash
pixi run gen-all   # from the repo root
```

## Mock mode

`VITE_MOCK=true` short-circuits all API queries to return hardcoded data from `src/api/queries.ts`. No backend required.

| Context                   | How mock mode is set                                                        |
| ------------------------- | --------------------------------------------------------------------------- |
| Local dev (`npm run dev`) | `.env.development` sets `VITE_MOCK=true`                                    |
| Docker image              | `ARG VITE_MOCK=true` in `Dockerfile` (default, override with `--build-arg`) |
| Production / compose      | Set `VITE_MOCK=false`                                                       |

The flag is baked into the bundle at build time — it cannot be changed at runtime.

## Local dev

```bash
cd services/frontend
npm install
npm run dev     # dev server on :3000, hot reload, mock mode on
```

To verify the production bundle locally (`VITE_MOCK=false`):

```bash
npm run build
npm run preview    # serves dist/ on :3000, no hot reload
```
