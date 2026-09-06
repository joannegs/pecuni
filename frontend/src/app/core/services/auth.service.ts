import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  GoogleLoginRequest,
  LoginRequest,
  RefreshRequest,
  RegisterRequest,
} from '../models/auth.model';
import { User } from '../models/user.model';
import { TokenService } from './token.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);

  private readonly baseUrl = environment.apiBaseUrl;

  readonly currentUser = signal<User | null>(null);
  readonly isAuthenticated = computed(() => !!this.tokenService.accessToken());

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/registrar`, request)
      .pipe(switchMap((response) => this.applyAuthResponseAndLoadUser(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/login`, request)
      .pipe(switchMap((response) => this.applyAuthResponseAndLoadUser(response)));
  }

  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    const request: GoogleLoginRequest = { idToken };
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/login/google`, request)
      .pipe(switchMap((response) => this.applyAuthResponseAndLoadUser(response)));
  }

  /**
   * Doesn't re-fetch /me on every refresh — identity doesn't change when
   * only the access token is renewed, and this runs from the interceptor's
   * silent-refresh path, so skipping it avoids a redundant call on every
   * request made right after the access token expires.
   */
  refresh(): Observable<AuthResponse> {
    const refreshToken = this.tokenService.refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('Nenhum refresh token disponível.'));
    }

    const request: RefreshRequest = { refreshToken };
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/auth/refresh`, request)
      .pipe(tap((response) => this.applyAuthResponse(response)));
  }

  logout(): Observable<void> {
    const request: RefreshRequest = { refreshToken: this.tokenService.refreshToken() ?? '' };

    return this.http.post<void>(`${this.baseUrl}/auth/logout`, request).pipe(
      catchError(() => of(void 0)),
      finalize(() => {
        this.tokenService.clear();
        this.currentUser.set(null);
      }),
    );
  }

  loadCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/me`).pipe(tap((user) => this.currentUser.set(user)));
  }

  private applyAuthResponseAndLoadUser(response: AuthResponse): Observable<AuthResponse> {
    this.applyAuthResponse(response);
    return this.loadCurrentUser().pipe(map(() => response));
  }

  private applyAuthResponse(response: AuthResponse): void {
    this.tokenService.setAccessToken(response.accessToken);
    this.tokenService.setRefreshToken(response.refreshToken);
  }
}
