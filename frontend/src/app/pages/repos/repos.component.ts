import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { GitRepo, CreateRepoRequest } from '../../models/repo.model';

@Component({
  selector: 'app-repos',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="container">
      <h1>PrismGit</h1>
      <p class="subtitle">Dashboard analítico que cuenta la historia visual de tus repos</p>
      <div class="card">
        <h2>Add Repository</h2>
        <form (ngSubmit)="addRepo()" class="repo-form">
          <input [(ngModel)]="newRepo.name" name="name" placeholder="Name" required />
          <input [(ngModel)]="newRepo.localPath" name="path" placeholder="Local path (optional)" />
          <input [(ngModel)]="newRepo.remoteUrl" name="url" placeholder="Remote URL (optional)" />
          <select [(ngModel)]="newRepo.provider" name="provider">
            <option value="">Provider...</option>
            <option value="NONE">None</option>
            <option value="GITHUB">GitHub</option>
            <option value="GITLAB">GitLab</option>
          </select>
          <button type="submit">Add</button>
        </form>
      </div>
      <div class="card">
        <h2>Repositories</h2>
        <div *ngIf="loading" class="empty">Loading...</div>
        <div *ngIf="!loading && repos.length === 0" class="empty">No repositories yet</div>
        <div *ngFor="let r of repos" class="repo-row">
          <div class="repo-info">
            <a [routerLink]="['/repo', r.id]" class="repo-name">{{ r.name }}</a>
            <span class="repo-path">{{ r.localPath || r.remoteUrl || '\u2014' }}</span>
          </div>
          <span class="repo-provider">{{ r.provider }}</span>
          <button (click)="deleteRepo(r.id)" class="btn-sm danger">Delete</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container { max-width: 800px; margin: 0 auto; padding: 2rem; }
    h1 { margin-bottom: 0; }
    .subtitle { color: #888; margin: 0.25rem 0 1.5rem; font-size: 0.9rem; }
    .card { background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 1.25rem; margin-bottom: 1.25rem; }
    .card h2 { margin: 0 0 0.75rem; font-size: 1rem; color: #555; }
    .repo-form { display: flex; flex-direction: column; gap: 0.5rem; }
    .repo-form input, .repo-form select { padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; }
    .repo-form button { padding: 0.5rem; background: #007bff; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
    .repo-row { display: flex; align-items: center; gap: 0.75rem; padding: 0.6rem 0; border-bottom: 1px solid #eee; }
    .repo-row:last-child { border-bottom: none; }
    .repo-info { flex: 1; display: flex; flex-direction: column; }
    .repo-name { font-weight: 600; text-decoration: none; color: #007bff; }
    .repo-name:hover { text-decoration: underline; }
    .repo-path { font-size: 0.8rem; color: #888; }
    .repo-provider { font-size: 0.7rem; background: #e9ecef; padding: 0.15rem 0.4rem; border-radius: 3px; color: #555; }
    .btn-sm { padding: 0.3rem 0.6rem; border: none; border-radius: 3px; cursor: pointer; font-size: 0.8rem; }
    .danger { background: #dc3545; color: #fff; }
    .empty { padding: 1.5rem; text-align: center; color: #888; font-size: 0.9rem; }
  `]
})
export class ReposComponent implements OnInit {
  repos: GitRepo[] = [];
  loading = true;
  newRepo: CreateRepoRequest = { name: '', localPath: '', remoteUrl: '', provider: 'NONE' };

  constructor(private api: ApiService) {}

  ngOnInit() { this.loadRepos(); }

  private onError(e: unknown) {
    console.error('API error:', e);
  }

  loadRepos() {
    this.loading = true;
    this.api.repos().subscribe({
      next: (r: GitRepo[]) => { this.repos = r; this.loading = false; },
      error: (e: unknown) => { this.onError(e); this.loading = false; }
    });
  }

  addRepo() {
    if (!this.newRepo.name) return;
    this.api.createRepo(this.newRepo).subscribe({
      next: () => {
        this.newRepo = { name: '', localPath: '', remoteUrl: '', provider: 'NONE' };
        this.loadRepos();
      },
      error: this.onError
    });
  }

  deleteRepo(id: string) {
    this.api.deleteRepo(id).subscribe({ next: () => this.loadRepos(), error: this.onError });
  }
}
