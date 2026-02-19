import { TestBed, ComponentFixture } from '@angular/core/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginComponent } from './login';
import { AuthService } from '../auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: { login: ReturnType<typeof vi.fn>; isLoggedIn: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = {
      login: vi.fn(),
      isLoggedIn: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('starts with empty fields and no error', () => {
    expect(component.username()).toBe('');
    expect(component.password()).toBe('');
    expect(component.loading()).toBe(false);
    expect(component.error()).toBeNull();
  });

  it('does not call login when username is empty', () => {
    component.password.set('secret');
    component.submit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('does not call login when password is empty', () => {
    component.username.set('admin');
    component.submit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('calls login with username and password', () => {
    authService.login.mockReturnValue(of(undefined));
    component.username.set('admin');
    component.password.set('secret');
    component.submit();
    expect(authService.login).toHaveBeenCalledWith('admin', 'secret');
  });

  it('navigates to / on successful login', () => {
    authService.login.mockReturnValue(of(undefined));
    component.username.set('admin');
    component.password.set('secret');
    const router = TestBed.inject(Router);
    const spy = vi.spyOn(router, 'navigate');
    component.submit();
    expect(spy).toHaveBeenCalledWith(['/']);
  });

  it('shows 401 error message on invalid credentials', () => {
    authService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 }))
    );
    component.username.set('admin');
    component.password.set('wrong');
    component.submit();
    expect(component.error()).toBe('Invalid username or password');
    expect(component.loading()).toBe(false);
  });

  it('shows generic error on non-401 failure', () => {
    authService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    component.username.set('admin');
    component.password.set('secret');
    component.submit();
    expect(component.error()).toBe('Login failed. Please try again.');
    expect(component.loading()).toBe(false);
  });

  it('sets loading to true while request is in flight', () => {
    let loadingDuringCall = false;
    authService.login.mockImplementation(() => {
      loadingDuringCall = component.loading();
      return of(undefined);
    });
    component.username.set('admin');
    component.password.set('secret');
    component.submit();
    expect(loadingDuringCall).toBe(true);
    expect(component.loading()).toBe(false);
  });
});
