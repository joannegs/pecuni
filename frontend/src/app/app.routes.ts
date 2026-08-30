import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  // accounts / categories / transactions / goals route groups are added
  // the same way as their respective feature is implemented (work plan,
  // weeks 3-6) — each lazy-loaded, none of them touching this file again.
  { path: '**', redirectTo: '' },
];
