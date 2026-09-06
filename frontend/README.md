# Pecuni — Frontend

Angular 20, fully standalone,feature-based folder.

## Structure

```
src/app
├── core/
├── shared/
├── features/
├── app.ts / app.html / app.css
├── app.config.ts
└── app.routes.ts
```

## Running locally

```bash
npm install
npm start
```

Requires the backend running on `http://localhost:8081` or the root `docker-compose.yml` once it exists.

## Deliberate decisions

**State management: signals, not NgRx classic.** Local and feature state is
plain Angular signals (`signal`/`computed`) owned by the relevant service or
component — no actions, reducers, or effects for state this app doesn't
have. `@ngrx/signals` (SignalStore) is a dependency, reserved for a feature
that genuinely needs complex state shared across several components (the
dashboard, transaction listings/filters) — not used yet, auth doesn't need
it. Reasoning: the Angular ecosystem itself has moved NgRx's own investment
toward the signals-based store, and classic Redux-style NgRx (actions +
reducers + effects boilerplate) doesn't pay for itself at this app's current
volume of cross-component shared state.
