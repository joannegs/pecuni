import { Injectable, signal } from '@angular/core';

/**
 * Holds the access token in memory as a signal — not localStorage. XSS in an
 * SPA can read localStorage; it can't read a value that only lives in JS
 * memory for the current page load. The refresh token is expected to live
 * in an httpOnly cookie set by the backend (see API contracts, section 1.2)
 * once /auth/refresh is implemented — this service only ever handles the
 * short-lived access token.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  readonly accessToken = signal<string | null>(null);

  setAccessToken(token: string | null): void {
    this.accessToken.set(token);
  }

  clear(): void {
    this.accessToken.set(null);
  }
}
