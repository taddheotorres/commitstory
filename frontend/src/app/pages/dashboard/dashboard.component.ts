import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { GitRepo } from '../../models/repo.model';
import { AnalyticsSummary, TimelinePoint, ActivityDistribution } from '../../models/analytics.model';
import { Story } from '../../models/story.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="container" *ngIf="repo">
      <a [routerLink]="['/']" class="back">&larr; Repositories</a>
      <h1>{{ repo.name }}</h1>
      <p class="meta">{{ repo.localPath || repo.remoteUrl || 'No path' }}</p>
      <div class="actions">
        <button (click)="sync()" [disabled]="syncing">{{ syncing ? 'Syncing...' : 'Sync' }}</button>
      </div>
      <div *ngIf="syncMsg" class="alert">{{ syncMsg }}</div>

      <div class="stats-row" *ngIf="summary">
        <div class="stat-card"><span class="stat-num">{{ summary.totalCommits }}</span><span class="stat-label">Commits</span></div>
        <div class="stat-card"><span class="stat-num">{{ summary.totalAuthors }}</span><span class="stat-label">Authors</span></div>
        <div class="stat-card"><span class="stat-num">{{ summary.totalFilesChanged }}</span><span class="stat-label">Files</span></div>
        <div class="stat-card"><span class="stat-num">{{ summary.firstCommit || '—' }}</span><span class="stat-label">From</span></div>
        <div class="stat-card"><span class="stat-num">{{ summary.lastCommit || '—' }}</span><span class="stat-label">To</span></div>
      </div>

      <div class="grid-2col">
        <div class="card" *ngIf="timeline.length">
          <h2>Commit Timeline</h2>
          <div class="timeline">
            <div *ngFor="let t of timeline" class="t-bar" [style.height.%]="barH(t)" [title]="t.date + ': ' + t.commitCount"></div>
          </div>
        </div>
        <div class="card" *ngIf="activityHour.length">
          <h2>Activity by Hour</h2>
          <div class="timeline">
            <div *ngFor="let a of activityHour" class="t-bar" [style.height.%]="hourH(a)" [title]="a.hour + 'h: ' + a.commitCount"></div>
          </div>
        </div>
      </div>

      <div class="grid-2col">
        <div class="card" *ngIf="summary?.topAuthors?.length">
          <h2>Top Authors</h2>
          <div *ngFor="let a of summary!.topAuthors" class="bar-row">
            <span class="bl">{{ a.name }}</span>
            <div class="bt"><div class="bf ab" [style.width.%]="pct(a.commitCount, maxAuthorCommits())"></div></div>
            <span class="bv">{{ a.commitCount }}</span>
          </div>
        </div>
        <div class="card" *ngIf="summary?.topFiles?.length">
          <h2>Most Changed Files</h2>
          <div *ngFor="let f of summary!.topFiles" class="bar-row">
            <span class="bl fl">{{ f.path }}</span>
            <div class="bt"><div class="bf fb" [style.width.%]="pct(f.changeCount, maxFileChanges())"></div></div>
            <span class="bv">{{ f.changeCount }}</span>
          </div>
        </div>
      </div>

      <div class="card" *ngIf="activityDay.length">
        <h2>Activity by Day</h2>
        <div class="dgrid">
          <div *ngFor="let d of activityDay" class="dcell" [style.background]="dayBg(d)">
            <span class="dn">{{ dayN(d.hour) }}</span>
            <span class="dc">{{ d.commitCount }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <h2>Generate Story</h2>
        <form (ngSubmit)="genStory()" class="sform">
          <select [(ngModel)]="storyMode" name="mode">
            <option value="TEMPLATE">Template</option>
            <option value="LLM">LLM</option>
          </select>
          <input [(ngModel)]="storyTitle" name="title" placeholder="Title" />
          <button type="submit">Generate</button>
        </form>
      </div>

      <div class="card" *ngIf="stories.length">
        <h2>Stories</h2>
        <div *ngFor="let s of stories" class="srow">
          <a [routerLink]="['/stories', s.id]" class="slink">{{ s.title }}</a>
          <span class="sbadge">{{ s.mode }}</span>
          <span class="sdate">{{ s.createdAt | date }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container { max-width: 960px; margin: 0 auto; padding: 2rem; }
    .back { text-decoration: none; color: #007bff; font-size: 0.9rem; }
    h1 { margin: 0.5rem 0 0; }
    .meta { color: #888; font-size: 0.85rem; margin: 0.15rem 0 1rem; }
    .actions button { padding: 0.5rem 1.2rem; background: #007bff; color: #fff; border: none; border-radius: 6px; cursor: pointer; }
    .actions button:disabled { background: #999; }
    .alert { padding: 0.75rem; background: #d4edda; border: 1px solid #c3e6cb; border-radius: 6px; margin-bottom: 1rem; font-size: 0.9rem; }

    .stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 0.75rem; margin-bottom: 1.5rem; }
    .stat-card { background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 1rem; text-align: center; }
    .stat-num { display: block; font-size: 1.3rem; font-weight: 700; }
    .stat-label { display: block; font-size: 0.7rem; color: #888; margin-top: 0.15rem; }

    .grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
    @media (max-width: 700px) { .grid-2col { grid-template-columns: 1fr; } }

    .card { background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 1.25rem; margin-bottom: 1.5rem; }
    .card h2 { margin: 0 0 0.75rem; font-size: 1rem; color: #555; }

    .timeline { display: flex; align-items: flex-end; gap: 2px; height: 110px; padding: 0.5rem 0; }
    .t-bar { flex: 1; background: #007bff; border-radius: 2px 2px 0 0; min-width: 2px; cursor: help; }

    .bar-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.4rem; }
    .bl { width: 90px; font-size: 0.8rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 0; }
    .fl { width: 150px; }
    .bt { flex: 1; height: 16px; background: #eee; border-radius: 3px; overflow: hidden; }
    .bf { height: 100%; border-radius: 3px; }
    .ab { background: #28a745; }
    .fb { background: #fd7e14; }
    .bv { font-size: 0.8rem; color: #666; width: 30px; text-align: right; flex-shrink: 0; }

    .dgrid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
    .dcell { border-radius: 6px; padding: 0.5rem; text-align: center; font-size: 0.8rem; }
    .dn { display: block; font-weight: 600; }
    .dc { display: block; font-size: 0.7rem; }

    .sform { display: flex; gap: 0.5rem; }
    .sform select, .sform input { padding: 0.4rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.85rem; }
    .sform button { padding: 0.4rem 1rem; background: #6f42c1; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
    .srow { display: flex; gap: 0.75rem; align-items: center; padding: 0.4rem 0; border-bottom: 1px solid #eee; font-size: 0.9rem; }
    .slink { flex: 1; color: #007bff; text-decoration: none; font-weight: 500; }
    .sbadge { font-size: 0.7rem; background: #e9ecef; padding: 0.15rem 0.4rem; border-radius: 3px; color: #555; }
    .sdate { font-size: 0.8rem; color: #888; }
  `]
})
export class DashboardComponent implements OnInit {
  repo?: GitRepo;
  summary?: AnalyticsSummary;
  timeline: TimelinePoint[] = [];
  activityHour: ActivityDistribution[] = [];
  activityDay: ActivityDistribution[] = [];
  stories: Story[] = [];
  syncing = false;
  syncMsg = '';
  storyMode = 'TEMPLATE';
  storyTitle = '';
  private rid = '';

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    this.rid = this.route.snapshot.paramMap.get('id')!;
    this.load();
  }

  private onError(e: unknown) {
    console.error('API error:', e);
  }

  load() {
    this.api.getRepo(this.rid).subscribe({ next: (r: GitRepo) => this.repo = r, error: this.onError });
    this.api.analyticsSummary(this.rid).subscribe({ next: (s: AnalyticsSummary) => this.summary = s, error: this.onError });
    this.api.analyticsTimeline(this.rid).subscribe({ next: (t: TimelinePoint[]) => this.timeline = t, error: this.onError });
    this.api.analyticsActivityHour(this.rid).subscribe({ next: (a: ActivityDistribution[]) => this.activityHour = a, error: this.onError });
    this.api.analyticsActivityDay(this.rid).subscribe({ next: (a: ActivityDistribution[]) => this.activityDay = a, error: this.onError });
    this.api.stories(this.rid).subscribe({ next: (s: Story[]) => this.stories = s, error: this.onError });
  }

  sync() {
    this.syncing = true;
    this.api.syncRepo(this.rid).subscribe({
      next: (r: { message: string }) => {
        this.syncMsg = r.message;
        this.syncing = false;
        this.load();
      },
      error: (e: unknown) => {
        this.onError(e);
        this.syncing = false;
      }
    });
  }

  genStory() {
    this.api.createStory(this.rid, { mode: this.storyMode, title: this.storyTitle || undefined })
      .subscribe({
        next: () => {
          this.storyTitle = '';
          this.api.stories(this.rid).subscribe({ next: (s: Story[]) => this.stories = s, error: this.onError });
        },
        error: this.onError
      });
  }

  barH(t: TimelinePoint): number {
    const mx = Math.max(...this.timeline.map((x: TimelinePoint) => x.commitCount), 1);
    return (t.commitCount / mx) * 100;
  }

  hourH(a: ActivityDistribution): number {
    const mx = Math.max(...this.activityHour.map((x: ActivityDistribution) => x.commitCount), 1);
    return (a.commitCount / mx) * 100;
  }

  maxAuthorCommits(): number {
    return Math.max(...this.summary!.topAuthors.map((x: { commitCount: number }) => x.commitCount), 1);
  }

  maxFileChanges(): number {
    return Math.max(...this.summary!.topFiles.map((x: { changeCount: number }) => x.changeCount), 1);
  }

  pct(v: number, mx: number): number {
    return (v / mx) * 100;
  }

  dayBg(d: ActivityDistribution): string {
    const mx = Math.max(...this.activityDay.map((x: ActivityDistribution) => x.commitCount), 1);
    const i = d.commitCount / mx;
    if (i === 0) return '#f8f9fa';
    if (i < 0.25) return '#c6e48b';
    if (i < 0.5) return '#7bc96f';
    if (i < 0.75) return '#239a3b';
    return '#196127';
  }

  dayN(n: number): string {
    return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][n - 1] || '';
  }
}
