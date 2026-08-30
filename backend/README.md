# Pecuni — Backend

Spring Boot 3 / Java 21 API. Package-by-feature, not package-by-layer.

## Structure

```
com.pecuni
├── config/
├── common/
│   ├── domain/
│   └── exception/
```

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

Swagger UI: `http://localhost:8080/docs`

## Profiles

| Profile | Purpose |
|---|---|
| `local` | Local development against a Dockerized Postgres |
| `test` | Integration tests — datasource injected by Testcontainers |
| `prod` | Deployed environment — all secrets from environment variables |

