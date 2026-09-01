# Pecuni — Backend

Spring Boot 3 / Java 21 API. Package-by-feature, not package-by-layer.

## Structure

```
com.pecuni
├── config/       # security chain, JWT filter, typed config properties
├── common/
│   ├── domain/   # BaseEntity — every entity extends this
│   └── exception/ # ApiExceptionHandler (RFC 7807) + shared exception types
├── auth/         # registration, login (local + Google), JWT/refresh tokens, /me
├── user/         # User entity — kept out of auth/ and workspace/ so neither
│                 # depends on the other (both depend on user/ instead)
└── workspace/    # Workspace / WorkspaceMember — transparent in the MVP,
                  # see decisions report §4.2
```

### Authentication

Access tokens are short-lived (15 min) JWTs (`auth.JwtService`, HS256).
Refresh tokens (7 days) are **not** JWTs — they're opaque random values,
stored only as a SHA-256 hash (`auth.RefreshTokenService`), so they can be
looked up, revoked and rotated on every use. Reuse of an already-revoked
refresh token revokes every other active refresh token for that user, as a
containment measure against token theft.

**Google login verification**: `auth.GoogleTokenVerifier` validates the
`idToken` sent by the frontend using Google's own `google-api-client`
library (`GoogleIdTokenVerifier`) rather than a hand-rolled JWKS
fetch/cache/verify — it's the library Google ships for exactly this,
handles certificate rotation and caching, and needs far less code than a
manual implementation. The expected audience is `pecuni.google.client-id`
(`GOOGLE_CLIENT_ID` env var in prod). Tests never hit Google's servers: the
verifier is unit-tested by mocking its `GoogleIdTokenVerifier` collaborator,
and the rest of the `/auth/login/google` flow is integration-tested by
mocking `GoogleTokenVerifier` itself.

## Running locally

From the repo root:

```bash
docker compose up backend db
```

Running outside Docker (Maven 3.9+ and Java 21 locally installed needed):

```bash
docker compose up -d db
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Swagger UI: `http://localhost:8081/docs`

A Postman collection for the auth endpoints (`/auth/*`, `/me`) is at
[`postman/Pecuni-Auth.postman_collection.json`](./postman/Pecuni-Auth.postman_collection.json)
— import it, it defaults `baseUrl` to `http://localhost:8081/api/v1` and
chains the access/refresh tokens between requests automatically.

## Profiles

| Profile | Purpose |
|---|---|
| `local` | Local development against a Dockerized Postgres |
| `test` | Integration tests — datasource injected by Testcontainers |
| `prod` | Deployed environment — all secrets from environment variables |

