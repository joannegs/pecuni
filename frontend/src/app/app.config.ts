import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Zoneless is deliberately NOT enabled yet — still opt-in/experimental
    // in Angular 20. Revisit once the app has enough surface to justify the
    // migration effort (see README).
    provideZoneChangeDetection({ eventCoalescing: true }),

    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),

    // No global store provider: state management is signals-based per
    // feature/service (see README — "Deliberate decisions"). @ngrx/signals
    // is pulled in for when a feature needs shared, non-trivial state
    // (dashboard, transactions) but isn't wired up anywhere yet.
  ],
};
