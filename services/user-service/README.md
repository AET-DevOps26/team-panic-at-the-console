# user-service

Authentication, user profiles, and role management. Owns the `users` database.

Port: **8084**

## Endpoints

| Method | Path             | Status                          |
| ------ | ---------------- | ------------------------------- |
| `GET`  | `/health`        | Implemented (scaffold)          |
| `POST` | `/auth/register` | Stub (501 via OpenAPI defaults) |
| `POST` | `/auth/login`    | Stub (501)                      |
| `POST` | `/auth/logout`   | Stub (501)                      |
| `GET`  | `/users/me`      | Stub (501)                      |
| `GET`  | `/users`         | Stub (501)                      |

Contract: `api/openapi.yaml` (tags `auth`, `users`). Session JWT + httpOnly cookie per [ADR 0007](../../docs/adr/0007-jwt-httponly-cookie.md).

## Local dev

```bash
cd services/user-service
pixi install
pixi run test
pixi run start
```

From the repo root:

```bash
pixi run --manifest-path services/user-service/pixi.toml test
```

Docker (build context is `services/`, same as incident-service):

```bash
docker build -f services/user-service/Dockerfile -t user-service:local services
```
