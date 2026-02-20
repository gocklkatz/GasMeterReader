import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let authService: { isLoggedIn: ReturnType<typeof vi.fn> };
  let router: { createUrlTree: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authService = { isLoggedIn: vi.fn() };
    router = { createUrlTree: vi.fn().mockReturnValue('fakeTree') };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('returns true when isLoggedIn() is true', () => {
    authService.isLoggedIn.mockReturnValue(true);
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('calls router.createUrlTree(["/login"]) and returns the tree when isLoggedIn() is false', () => {
    authService.isLoggedIn.mockReturnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).toBe('fakeTree');
  });
});
