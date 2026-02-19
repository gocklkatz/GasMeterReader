import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  it('isLoggedIn is false initially when sessionStorage is empty', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  it('login posts credentials and stores token', () => {
    service.login('admin', 'changeme').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'admin', password: 'changeme' });
    req.flush({ token: 'test-token' });

    expect(service.isLoggedIn()).toBe(true);
    expect(sessionStorage.getItem('auth_token')).toBe('test-token');
  });

  it('logout clears token and sets isLoggedIn to false', () => {
    service.login('admin', 'changeme').subscribe();
    httpMock.expectOne('http://localhost:8080/auth/login').flush({ token: 'test-token' });

    service.logout();

    expect(service.isLoggedIn()).toBe(false);
    expect(sessionStorage.getItem('auth_token')).toBeNull();
  });

  it('getToken returns current token after login', () => {
    service.login('admin', 'changeme').subscribe();
    httpMock.expectOne('http://localhost:8080/auth/login').flush({ token: 'my-token' });

    expect(service.getToken()).toBe('my-token');
  });

  it('getToken returns null when logged out', () => {
    expect(service.getToken()).toBeNull();
  });

  it('isLoggedIn is true when sessionStorage has a token at construction', () => {
    sessionStorage.setItem('auth_token', 'stored-token');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    const newService = TestBed.inject(AuthService);
    expect(newService.isLoggedIn()).toBe(true);
    TestBed.inject(HttpTestingController).verify();
  });
});
