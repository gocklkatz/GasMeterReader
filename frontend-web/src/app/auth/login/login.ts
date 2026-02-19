import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  username = signal('');
  password = signal('');
  loading = signal(false);
  error = signal<string | null>(null);

  submit(): void {
    if (!this.username() || !this.password()) return;
    this.loading.set(true);
    this.error.set(null);

    this.authService.login(this.username(), this.password()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(
          err.status === 401
            ? 'Invalid username or password'
            : 'Login failed. Please try again.'
        );
      }
    });
  }
}
