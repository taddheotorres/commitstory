import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { GitRepo, CreateRepoRequest } from '../models/repo.model';
import { CommitEntry } from '../models/commit.model';
import { Story, CreateStoryRequest } from '../models/story.model';
import { AnalyticsSummary, TimelinePoint, ActivityDistribution } from '../models/analytics.model';
import { SyncResponse } from '../models/sync.model';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  const mockRepo: GitRepo = {
    id: '1',
    name: 'test-repo',
    localPath: '/test/path',
    remoteUrl: 'https://github.com/test/repo',
    provider: 'GITHUB',
    createdAt: new Date().toISOString()
  };

  const mockCommit: CommitEntry = {
    id: '1',
    sha: 'abc123',
    authorName: 'Alice',
    authorEmail: 'alice@example.com',
    authoredAt: new Date().toISOString(),
    message: 'feat: add feature',
    files: ['src/main.ts'],
    additions: 10,
    deletions: 2
  };

  const mockStory: Story = {
    id: '1',
    repoId: '1',
    title: 'Test Story',
    content: '# Story Content',
    mode: 'TEMPLATE',
    startSha: null,
    endSha: null,
    createdAt: new Date().toISOString()
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Repositories', () => {
    it('should get all repos', () => {
      const mockRepos = [mockRepo];

      service.repos().subscribe(repos => {
        expect(repos.length).toBe(1);
        expect(repos[0].name).toBe('test-repo');
      });

      const req = httpMock.expectOne('/api/repos');
      expect(req.request.method).toBe('GET');
      req.flush(mockRepos);
    });

    it('should create repo', () => {
      const createRequest: CreateRepoRequest = {
        name: 'new-repo',
        localPath: '/path',
        remoteUrl: 'https://github.com/user/repo',
        provider: 'GITHUB'
      };

      service.createRepo(createRequest).subscribe(repo => {
        expect(repo.id).toBe('1');
        expect(repo.name).toBe('test-repo');
      });

      const req = httpMock.expectOne('/api/repos');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockRepo);
    });

    it('should get repo by id', () => {
      service.getRepo('1').subscribe(repo => {
        expect(repo.id).toBe('1');
        expect(repo.name).toBe('test-repo');
      });

      const req = httpMock.expectOne('/api/repos/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockRepo);
    });

    it('should delete repo', () => {
      service.deleteRepo('1').subscribe();

      const req = httpMock.expectOne('/api/repos/1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should sync repo', () => {
      const syncResponse: SyncResponse = { commitsAdded: 5, message: 'Synced 5 commits' };

      service.syncRepo('1').subscribe(response => {
        expect(response.commitsAdded).toBe(5);
      });

      const req = httpMock.expectOne('/api/repos/1/sync');
      expect(req.request.method).toBe('POST');
      req.flush(syncResponse);
    });
  });

  describe('Commits', () => {
    it('should get commits with default pagination', () => {
      const mockCommits = [mockCommit];

      service.commits('1').subscribe(commits => {
        expect(commits.length).toBe(1);
        expect(commits[0].sha).toBe('abc123');
      });

      const req = httpMock.expectOne(request => request.url === '/api/repos/1/commits');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('50');
      req.flush(mockCommits);
    });

    it('should get commits with custom pagination', () => {
      service.commits('1', 2, 25).subscribe();

      const req = httpMock.expectOne(request => request.url === '/api/repos/1/commits');
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('25');
      req.flush([]);
    });
  });

  describe('Analytics', () => {
    it('should get analytics summary', () => {
      const summary: AnalyticsSummary = {
        totalCommits: 10,
        totalAuthors: 2,
        totalFilesChanged: 5,
        firstCommit: '2024-01-01',
        lastCommit: '2024-01-31',
        topAuthors: [],
        topFiles: []
      };

      service.analyticsSummary('1').subscribe(s => {
        expect(s.totalCommits).toBe(10);
        expect(s.totalAuthors).toBe(2);
      });

      const req = httpMock.expectOne('/api/repos/1/analytics/summary');
      expect(req.request.method).toBe('GET');
      req.flush(summary);
    });

    it('should get timeline', () => {
      const timeline: TimelinePoint[] = [
        { date: '2024-01-01', commitCount: 5 },
        { date: '2024-01-02', commitCount: 3 }
      ];

      service.analyticsTimeline('1').subscribe(points => {
        expect(points.length).toBe(2);
        expect(points[0].commitCount).toBe(5);
      });

      const req = httpMock.expectOne('/api/repos/1/analytics/timeline');
      expect(req.request.method).toBe('GET');
      req.flush(timeline);
    });

    it('should get activity by hour', () => {
      const activity: ActivityDistribution[] = Array.from({ length: 24 }, (_, i) => ({
        hour: i,
        commitCount: i === 10 ? 5 : 0
      }));

      service.analyticsActivityHour('1').subscribe(dist => {
        expect(dist.length).toBe(24);
        expect(dist[10].commitCount).toBe(5);
      });

      const req = httpMock.expectOne('/api/repos/1/analytics/activity/hour');
      expect(req.request.method).toBe('GET');
      req.flush(activity);
    });

    it('should get activity by day', () => {
      const activity: ActivityDistribution[] = [
        { hour: 0, commitCount: 2 },
        { hour: 1, commitCount: 3 }
      ];

      service.analyticsActivityDay('1').subscribe(dist => {
        expect(dist.length).toBe(2);
      });

      const req = httpMock.expectOne('/api/repos/1/analytics/activity/day');
      expect(req.request.method).toBe('GET');
      req.flush(activity);
    });
  });

  describe('Stories', () => {
    it('should create story', () => {
      const createRequest: CreateStoryRequest = {
        mode: 'TEMPLATE',
        title: 'Test Story',
        startSha: null,
        endSha: null
      };

      service.createStory('1', createRequest).subscribe(story => {
        expect(story.id).toBe('1');
        expect(story.title).toBe('Test Story');
      });

      const req = httpMock.expectOne('/api/repos/1/stories');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockStory);
    });

    it('should get stories for repo', () => {
      const mockStories = [mockStory];

      service.stories('1').subscribe(stories => {
        expect(stories.length).toBe(1);
        expect(stories[0].title).toBe('Test Story');
      });

      const req = httpMock.expectOne('/api/repos/1/stories');
      expect(req.request.method).toBe('GET');
      req.flush(mockStories);
    });

    it('should get story by id', () => {
      service.getStory('1').subscribe(story => {
        expect(story.id).toBe('1');
        expect(story.mode).toBe('TEMPLATE');
      });

      const req = httpMock.expectOne('/api/stories/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockStory);
    });
  });

  it('should handle HTTP errors', () => {
    service.repos().subscribe(
      () => fail('should have failed'),
      error => {
        expect(error.status).toBe(500);
      }
    );

    const req = httpMock.expectOne('/api/repos');
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('should use correct base URL', () => {
    service.repos().subscribe();
    const req = httpMock.expectOne(request => request.url.includes('/api'));
    expect(req.request.url).toContain('/api/repos');
    req.flush([]);
  });
});
