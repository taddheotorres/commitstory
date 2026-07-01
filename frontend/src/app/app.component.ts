import { Component } from '@angular/core';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, CommonModule],
  template: `
    <nav class="navbar" *ngIf="auth.isLoggedIn()">
      <a [routerLink]="['/']" class="brand">PrismGit</a>
      <div class="nav-right">
        <span class="user-name">{{ auth.getUser()?.name }}</span>
        <button (click)="logout()" class="btn-logout">Logout</button>
      </div>
    </nav>
    <router-outlet />
  `,
  styles: [`
    .navbar { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #ddd; padding: 0.6rem 1.5rem; }
    .brand { font-weight: 700; font-size: 1rem; text-decoration: none; color: #333; }
    .nav-right { display: flex; align-items: center; gap: 0.75rem; }
    .user-name { font-size: 0.85rem; color: #666; }
    .btn-logout { padding: 0.3rem 0.6rem; background: none; border: 1px solid #ccc; border-radius: 4px; cursor: pointer; font-size: 0.8rem; color: #555; }
    .btn-logout:hover { background: #f0f0f0; }
  `]
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
