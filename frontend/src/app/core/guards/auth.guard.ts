import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

// Reads TokenService directly rather than AuthService.isAuthenticated():
// the guard only needs a boolean presence check, and TokenService is a
// leaf dependency (no HttpClient, no requests). Depending on AuthService
// here would pull the guard into its larger surface (HTTP calls, user
// state) for no benefit and add a second, unnecessary import path into it.
export const authGuard: CanActivateFn = () => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.accessToken()) {
    return true;
  }

  return router.parseUrl('/login');
};
