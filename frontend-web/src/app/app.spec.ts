import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { vi } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService } from './auth/auth.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        provideHttpClient(),
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should contain a router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });

  it('should render a nav with two links', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const links = fixture.nativeElement.querySelectorAll('nav a');
    expect(links).toHaveLength(2);
  });

  it('nav links point to / and /browse', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const links: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('nav a');
    expect(links[0].getAttribute('href')).toBe('/');
    expect(links[1].getAttribute('href')).toBe('/browse');
  });

  it('sign-out button is not shown when logged out', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('.nav__logout');
    expect(button).toBeNull();
  });

  describe('when logged in', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [App],
        providers: [
          provideRouter(routes),
          provideHttpClient(),
          {
            provide: AuthService,
            useValue: { isLoggedIn: vi.fn().mockReturnValue(true), logout: vi.fn() },
          },
        ],
      }).compileComponents();
    });

    it('shows the sign-out button when logged in', () => {
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('.nav__logout');
      expect(button).not.toBeNull();
    });

    it('logout() calls authService.logout() and navigates to /login', async () => {
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      const authService = TestBed.inject(AuthService) as any;
      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      fixture.componentInstance.logout();
      expect(authService.logout).toHaveBeenCalled();
      expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    });
  });
});
