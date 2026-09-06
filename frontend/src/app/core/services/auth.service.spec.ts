import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { TokenService } from './token.service';

describe('AuthService', () => {
  let service: AuthService;
  let tokenService: TokenService;
  let httpMock: HttpTestingController;

  const baseUrl = environment.apiBaseUrl;
  const authResponse = { accessToken: 'access-1', refreshToken: 'refresh-1', expiresIn: 900 };
  const user = { id: 'user-1', nome: 'Ana Souza', email: 'ana@email.com', provider: 'LOCAL' as const };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    tokenService = TestBed.inject(TokenService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function expectMeCall(): void {
    const meReq = httpMock.expectOne(`${baseUrl}/me`);
    expect(meReq.request.method).toBe('GET');
    meReq.flush(user);
  }

  it('register() posts to /auth/registrar, stores tokens and loads the current user', () => {
    let result: unknown;
    service.register({ nome: 'Ana Souza', email: 'ana@email.com', senha: 'S3nhaForte!' }).subscribe((response) => {
      result = response;
    });

    const req = httpMock.expectOne(`${baseUrl}/auth/registrar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Ana Souza', email: 'ana@email.com', senha: 'S3nhaForte!' });
    req.flush(authResponse);

    expectMeCall();

    expect(result).toEqual(authResponse);
    expect(tokenService.accessToken()).toBe('access-1');
    expect(tokenService.refreshToken()).toBe('refresh-1');
    expect(service.currentUser()).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('register() with a duplicate e-mail surfaces the 409 problem detail', () => {
    let error: unknown;
    service.register({ nome: 'Ana Souza', email: 'ana@email.com', senha: 'S3nhaForte!' }).subscribe({
      error: (err) => (error = err),
    });

    const req = httpMock.expectOne(`${baseUrl}/auth/registrar`);
    req.flush(
      { type: 'email-already-registered', title: 'Violação de regra de negócio', status: 409, detail: 'Este e-mail já está cadastrado.' },
      { status: 409, statusText: 'Conflict' },
    );

    expect((error as { status: number }).status).toBe(409);
    expect(tokenService.accessToken()).toBeNull();
  });

  it('login() posts to /auth/login, stores tokens and loads the current user', () => {
    service.login({ email: 'ana@email.com', senha: 'S3nhaForte!' }).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana@email.com', senha: 'S3nhaForte!' });
    req.flush(authResponse);

    expectMeCall();

    expect(tokenService.accessToken()).toBe('access-1');
    expect(service.currentUser()).toEqual(user);
  });

  it('login() with wrong credentials surfaces the 401 problem detail and stores nothing', () => {
    let error: unknown;
    service.login({ email: 'ana@email.com', senha: 'senha-errada' }).subscribe({
      error: (err) => (error = err),
    });

    const req = httpMock.expectOne(`${baseUrl}/auth/login`);
    req.flush(
      { type: 'nao-autenticado', title: 'Não autenticado', status: 401, detail: 'E-mail ou senha inválidos.' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect((error as { status: number }).status).toBe(401);
    expect(tokenService.accessToken()).toBeNull();
  });

  it('loginWithGoogle() posts the idToken to /auth/login/google and loads the current user', () => {
    service.loginWithGoogle('google-id-token').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/login/google`);
    expect(req.request.body).toEqual({ idToken: 'google-id-token' });
    req.flush(authResponse);

    expectMeCall();

    expect(tokenService.accessToken()).toBe('access-1');
  });

  it('refresh() sends the stored refresh token and updates both tokens, without reloading /me', () => {
    tokenService.setRefreshToken('refresh-0');

    let result: unknown;
    service.refresh().subscribe((response) => (result = response));

    const req = httpMock.expectOne(`${baseUrl}/auth/refresh`);
    expect(req.request.body).toEqual({ refreshToken: 'refresh-0' });
    req.flush(authResponse);

    expect(result).toEqual(authResponse);
    expect(tokenService.accessToken()).toBe('access-1');
    expect(tokenService.refreshToken()).toBe('refresh-1');
    httpMock.expectNone(`${baseUrl}/me`);
  });

  it('refresh() fails fast without hitting the network when there is no refresh token', () => {
    let error: unknown;
    service.refresh().subscribe({ error: (err) => (error = err) });

    expect(error).toBeInstanceOf(Error);
    httpMock.expectNone(`${baseUrl}/auth/refresh`);
  });

  it('logout() clears tokens and currentUser even when the request fails', () => {
    tokenService.setAccessToken('access-1');
    tokenService.setRefreshToken('refresh-1');
    service.currentUser.set(user);

    let completed = false;
    service.logout().subscribe(() => (completed = true));

    const req = httpMock.expectOne(`${baseUrl}/auth/logout`);
    expect(req.request.body).toEqual({ refreshToken: 'refresh-1' });
    req.flush(null, { status: 500, statusText: 'Internal Server Error' });

    expect(completed).toBe(true);
    expect(tokenService.accessToken()).toBeNull();
    expect(tokenService.refreshToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('loadCurrentUser() populates currentUser from GET /me', () => {
    service.loadCurrentUser().subscribe();

    expectMeCall();

    expect(service.currentUser()).toEqual(user);
  });
});
