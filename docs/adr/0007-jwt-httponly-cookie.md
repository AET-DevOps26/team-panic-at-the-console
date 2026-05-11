# JWT stored in httpOnly cookie, not localStorage

`user-service` issues a JWT on login. The browser stores it as an `httpOnly`, `SameSite=Strict` cookie. The gateway reads the cookie on each request, validates the signature, and injects `X-User-Id` / `X-User-Role` headers for downstream services. JavaScript cannot access the token.

The default web dev approach is to store JWTs in `localStorage` and attach them as `Authorization: Bearer` headers. We rejected this because `localStorage` is readable by any JavaScript running on the page — including third-party libraries and XSS payloads — making token theft trivial. An `httpOnly` cookie is inaccessible to JS by design. `SameSite=Strict` mitigates CSRF without requiring a separate CSRF token since the application does not use cross-origin form submissions.
