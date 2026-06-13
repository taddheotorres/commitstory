import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GitRepo, CreateRepoRequest } from '../models/repo.model';
import { CommitEntry } from '../models/commit.model';
import { Story, CreateStoryRequest } from '../models/story.model';
import { AnalyticsSummary, TimelinePoint, ActivityDistribution } from '../models/analytics.model';
import { SyncResponse } from '../models/sync.model';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  repos(): Observable<GitRepo[]> {
    return this.http.get<GitRepo[]>(`${this.base}/repos`);
  }

  createRepo(req: CreateRepoRequest): Observable<GitRepo> {
    return this.http.post<GitRepo>(`${this.base}/repos`, req);
  }

  getRepo(id: string): Observable<GitRepo> {
    return this.http.get<GitRepo>(`${this.base}/repos/${id}`);
  }

  deleteRepo(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/repos/${id}`);
  }

  syncRepo(id: string): Observable<SyncResponse> {
    return this.http.post<SyncResponse>(`${this.base}/repos/${id}/sync`, {});
  }

  commits(id: string, page = 0, size = 50): Observable<CommitEntry[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<CommitEntry[]>(`${this.base}/repos/${id}/commits`, { params });
  }

  analyticsSummary(id: string): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${this.base}/repos/${id}/analytics/summary`);
  }

  analyticsTimeline(id: string): Observable<TimelinePoint[]> {
    return this.http.get<TimelinePoint[]>(`${this.base}/repos/${id}/analytics/timeline`);
  }

  analyticsActivityHour(id: string): Observable<ActivityDistribution[]> {
    return this.http.get<ActivityDistribution[]>(`${this.base}/repos/${id}/analytics/activity/hour`);
  }

  analyticsActivityDay(id: string): Observable<ActivityDistribution[]> {
    return this.http.get<ActivityDistribution[]>(`${this.base}/repos/${id}/analytics/activity/day`);
  }

  createStory(repoId: string, req: CreateStoryRequest): Observable<Story> {
    return this.http.post<Story>(`${this.base}/repos/${repoId}/stories`, req);
  }

  stories(repoId: string): Observable<Story[]> {
    return this.http.get<Story[]>(`${this.base}/repos/${repoId}/stories`);
  }

  getStory(id: string): Observable<Story> {
    return this.http.get<Story>(`${this.base}/stories/${id}`);
  }
}
