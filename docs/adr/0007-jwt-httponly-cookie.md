# JWT stored in httpOnly cookie, not localStorage

`user-service` issues a JWT on login. The browser stores it as an `httpOnly`, `SameSite=Strict` cookie. The gateway reads the cookie on each request, validates the signature, and injects `X-User-Id` / `X-User-Role` headers for downstream services. JavaScript cannot access the token.

The default web dev approach is to store JWTs in `localStorage` and attach them as `Authorization: Bearer` headers. We rejected this because `localStorage` is readable by any JavaScript running on the page — including third-party libraries and XSS payloads — making token theft trivial. An `httpOnly` cookie is inaccessible to JS by design. `SameSite=Strict` mitigates CSRF without requiring a separate CSRF token since the application does not use cross-origin form submissions.

**OpenAPI modeling**: Protected user routes (`/users/me`, `/users`) document the `session` cookie requirement in operation descriptions and set `security: []` so codegen does not attach Bearer auth. They must **not** reference a `sessionCookie` OpenAPI security scheme: `openapi-python-client` maps that to `AuthenticatedClient` with a mandatory `Authorization: Bearer` token. Runtime auth is still enforced by user-service / gateway via the cookie (or internal `X-User-Id` / `X-User-Role` headers).
