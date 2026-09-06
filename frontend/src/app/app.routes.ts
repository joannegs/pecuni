import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'signup',
    loadComponent: () => import('./pages/auth/signup/signup').then((m) => m.Signup),
  },
  { path: '**', redirectTo: 'login' },
];
