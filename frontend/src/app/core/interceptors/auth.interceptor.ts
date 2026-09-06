import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';

// A 401 from these is a credential/token error the caller must handle
// itself, never a sign that the access token expired — retrying them
// through authService.refresh() would either be wrong (login/registrar:
// wrong password isn't fixed by refreshing) or recurse forever (refresh:
// a 401 there means the refresh token itself is invalid).
const AUTH_ENDPOINTS_WITHOUT_REFRESH = ['/auth/login', '/auth/registrar', '/auth/refresh'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = tokenService.accessToken();
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      const isExemptEndpoint = AUTH_ENDPOINTS_WITHOUT_REFRESH.some((path) => req.url.includes(path));
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isExemptEndpoint) {
        return throwError(() => error);
      }

      // Simplified on purpose: each 401 triggers its own refresh call rather
      // than sharing one in-flight refresh across concurrent requests. A
      // burst of parallel 401s will fire a burst of redundant refresh calls
      // instead of one — acceptable for this app's current traffic; a
      // shared/queued refresh is the fix if that ever becomes a problem.
      return authService.refresh().pipe(
        switchMap(() => {
          const newToken = tokenService.accessToken();
          return next(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } }));
        }),
        catchError((refreshError: unknown) => {
          router.navigateByUrl('/login');
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
