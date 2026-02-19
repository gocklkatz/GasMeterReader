import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private apiBase = 'http://localhost:8080';

  private token = signal<string | null>(sessionStorage.getItem('auth_token'));

  isLoggedIn = computed(() => this.token() !== null);

  login(username: string, password: string): Observable<void> {
    return this.http
      .post<{ token: string }>(`${this.apiBase}/auth/login`, { username, password })
      .pipe(
        tap(response => {
          sessionStorage.setItem('auth_token', response.token);
          this.token.set(response.token);
        }),
        map(() => undefined)
      );
  }

  logout(): void {
    sessionStorage.removeItem('auth_token');
    this.token.set(null);
  }

  getToken(): string | null {
    return this.token();
  }
}
