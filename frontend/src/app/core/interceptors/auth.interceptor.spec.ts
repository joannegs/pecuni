import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { TokenService } from '../services/token.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokenService: TokenService;
  let router: jasmine.SpyObj<Router>;

  const baseUrl = environment.apiBaseUrl;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('refreshes the token once on a 401 and repeats the original request with the new token', () => {
    tokenService.setAccessToken('expired-token');
    tokenService.setRefreshToken('refresh-1');
    const user = { id: 'user-1', nome: 'Ana Souza', email: 'ana@email.com', provider: 'LOCAL' };

    let result: unknown;
    http.get(`${baseUrl}/me`).subscribe((response) => (result = response));

    const firstAttempt = httpMock.expectOne(`${baseUrl}/me`);
    expect(firstAttempt.request.headers.get('Authorization')).toBe('Bearer expired-token');
    firstAttempt.flush(null, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne(`${baseUrl}/auth/refresh`);
    expect(refreshReq.request.body).toEqual({ refreshToken: 'refresh-1' });
    refreshReq.flush({ accessToken: 'new-token', refreshToken: 'refresh-2', expiresIn: 900 });

    const retriedAttempt = httpMock.expectOne(`${baseUrl}/me`);
    expect(retriedAttempt.request.headers.get('Authorization')).toBe('Bearer new-token');
    retriedAttempt.flush(user);

    expect(result).toEqual(user);
    expect(tokenService.accessToken()).toBe('new-token');
  });

  it('does not attempt a refresh for a 401 returned by /auth/login itself', () => {
    let error: { status: number } | undefined;
    http.post(`${baseUrl}/auth/login`, { email: 'ana@email.com', senha: 'errada' }).subscribe({
      error: (err) => (error = err),
    });

    const req = httpMock.expectOne(`${baseUrl}/auth/login`);
    req.flush({ detail: 'E-mail ou senha inválidos.' }, { status: 401, statusText: 'Unauthorized' });

    expect(error?.status).toBe(401);
    httpMock.expectNone(`${baseUrl}/auth/refresh`);
  });

  it('redirects to /login when the refresh itself fails', () => {
    tokenService.setAccessToken('expired-token');
    tokenService.setRefreshToken('refresh-1');

    let error: { status: number } | undefined;
    http.get(`${baseUrl}/me`).subscribe({ error: (err) => (error = err) });

    httpMock.expectOne(`${baseUrl}/me`).flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${baseUrl}/auth/refresh`)
      .flush({ detail: 'Refresh token inválido.' }, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    expect(error?.status).toBe(401);
  });
});
