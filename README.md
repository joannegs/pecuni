# Pecuni

Personal finance management app — portfolio project. Java 21 / Spring Boot
backend, Angular 20 frontend, PostgreSQL.

## Quick start

```bash
docker compose up
```

That's the entire setup: Postgres, backend (`localhost:8080`, Swagger at
`/docs`) and frontend (`localhost:4200`) all start together, with source
mounted for hot reload on both sides.

### First clone

```bash
npm install
```

## Repository layout

```
pecuni/
├── backend/     # Spring Boot API
├── frontend/    # Angular 20 SPA
└── docker-compose.yml
```

Mono-repo by design: a single PR can carry an API contract change and its
frontend consumption together, and a single `docker compose up` boots the
whole stack. CI runs backend and frontend jobs independently, gated by path
filters (`backend/**` / `frontend/**`), so neither pipeline runs on a change
that couldn't affect it.

## License

MIT — see [`LICENSE`](./LICENSE).
