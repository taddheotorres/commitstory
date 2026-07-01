import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container">
      <div class="card">
        <h1>PrismGit</h1>
        <h2>Create Account</h2>
        <form (ngSubmit)="register()" class="auth-form">
          <input [(ngModel)]="name" name="name" placeholder="Name" required />
          <input [(ngModel)]="email" name="email" type="email" placeholder="Email" required />
          <input [(ngModel)]="password" name="password" type="password" placeholder="Password (min 6 chars)" required />
          <button type="submit" [disabled]="loading">{{ loading ? 'Creating account...' : 'Register' }}</button>
          <p *ngIf="error" class="error">{{ error }}</p>
        </form>
        <p class="switch">Already have an account? <a routerLink="/login">Sign In</a></p>
      </div>
    </div>
  `,
  styles: [`
    .container { max-width: 400px; margin: 4rem auto; padding: 0 1rem; }
    .card { background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 2rem; text-align: center; }
    h1 { margin: 0 0 0.25rem; font-size: 1.5rem; }
    h2 { margin: 0 0 1.25rem; font-size: 1rem; color: #555; font-weight: 400; }
    .auth-form { display: flex; flex-direction: column; gap: 0.75rem; }
    .auth-form input { padding: 0.6rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; }
    .auth-form button { padding: 0.6rem; background: #28a745; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
    .auth-form button:disabled { background: #999; }
    .error { color: #dc3545; font-size: 0.85rem; margin: 0; }
    .switch { margin-top: 1rem; font-size: 0.85rem; color: #666; }
    .switch a { color: #007bff; text-decoration: none; }
  `]
})
export class RegisterComponent {
  name = '';
  email = '';
  password = '';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  register(): void {
    if (!this.name || !this.email || !this.password) return;
    this.loading = true;
    this.error = '';
    this.auth.register({ name: this.name, email: this.email, password: this.password }).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.error = err.error?.error || 'Registration failed';
        this.loading = false;
      }
    });
  }
}
