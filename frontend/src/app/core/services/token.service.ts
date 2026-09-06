import { Injectable, signal } from '@angular/core';

/**
 * Holds both tokens in memory as signals — not localStorage. XSS in an SPA
 * can read localStorage; it can't read a value that only lives in JS memory
 * for the current page load.
 *
 * The backend (com.pecuni.auth) does not set an httpOnly refresh cookie —
 * /auth/refresh and /auth/logout both take the refresh token explicitly in
 * the request body, and it's returned in the body of every auth response.
 * So, deliberately, the refresh token lives here too, in memory only, same
 * as the access token: a full page reload or closed tab loses the session
 * and requires logging in again. That's an accepted trade-off for now
 * rather than weakening the anti-XSS stance by putting it in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  readonly accessToken = signal<string | null>(null);
  readonly refreshToken = signal<string | null>(null);

  setAccessToken(token: string | null): void {
    this.accessToken.set(token);
  }

  setRefreshToken(token: string | null): void {
    this.refreshToken.set(token);
  }

  clear(): void {
    this.accessToken.set(null);
    this.refreshToken.set(null);
  }
}
