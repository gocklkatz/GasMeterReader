import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { App } from './app';
import { routes } from './app.routes';

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
});
