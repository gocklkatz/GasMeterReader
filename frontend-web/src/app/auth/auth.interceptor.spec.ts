import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: { getToken: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authService = { getToken: vi.fn(), logout: vi.fn() };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('adds Authorization: Bearer <token> header when token exists', () => {
    authService.getToken.mockReturnValue('my-token');
    httpClient.get('/test').subscribe();
    const req = httpTesting.expectOne('/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush({});
  });

  it('does not add Authorization header when no token', () => {
    authService.getToken.mockReturnValue(null);
    httpClient.get('/test').subscribe();
    const req = httpTesting.expectOne('/test');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('calls authService.logout() and router.navigate(["/login"]) on 401 response, and re-throws error', () => {
    authService.getToken.mockReturnValue(null);
    let caughtError: any;
    httpClient.get('/test').subscribe({ error: e => (caughtError = e) });
    const req = httpTesting.expectOne('/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(caughtError).toBeTruthy();
  });

  it('re-throws non-401 errors without calling logout', () => {
    authService.getToken.mockReturnValue(null);
    let caughtError: any;
    httpClient.get('/test').subscribe({ error: e => (caughtError = e) });
    const req = httpTesting.expectOne('/test');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    expect(authService.logout).not.toHaveBeenCalled();
    expect(caughtError).toBeTruthy();
  });
});
