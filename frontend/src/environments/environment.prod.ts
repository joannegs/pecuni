export const environment = {
  production: true,
  // Injected at build time in CI (work plan, week 8) — never a localhost fallback in prod.
  apiBaseUrl: '/api/v1',
  // TODO(joanne): the pasted client_secret_*.json only registers
  // http://localhost:4200 as an authorized origin — it's dev-only. Create a
  // separate OAuth client in the "pecuni" Google Cloud project with the real
  // prod origin registered, then replace this with its client ID.
  googleClientId: 'REPLACE_WITH_GOOGLE_OAUTH_CLIENT_ID',
};
