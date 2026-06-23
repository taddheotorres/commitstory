import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { ApiService } from '../../services/api.service';
import { GitRepo } from '../../models/repo.model';
import { AnalyticsSummary, TimelinePoint, ActivityDistribution } from '../../models/analytics.model';
import { Story } from '../../models/story.model';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let activatedRoute: any;

  const mockRepo: GitRepo = {
    id: 'test-id',
    name: 'test-repo',
    localPath: '/test/path',
    remoteUrl: 'https://github.com/test/repo',
    provider: 'GITHUB',
    createdAt: new Date().toISOString()
  };

  const mockSummary: AnalyticsSummary = {
    totalCommits: 10,
    totalAuthors: 2,
    totalFilesChanged: 5,
    firstCommit: '2024-01-01',
    lastCommit: '2024-01-31',
    topAuthors: [
      { name: 'Alice', email: 'alice@example.com', commitCount: 6 },
      { name: 'Bob', email: 'bob@example.com', commitCount: 4 }
    ],
    topFiles: [
      { path: 'src/main.ts', changeCount: 15 },
      { path: 'src/app.ts', changeCount: 8 }
    ]
  };

  const mockTimeline: TimelinePoint[] = [
    { date: '2024-01-01', commitCount: 3 },
    { date: '2024-01-15', commitCount: 5 },
    { date: '2024-01-31', commitCount: 2 }
  ];

  const mockActivityHour: ActivityDistribution[] = Array.from({ length: 24 }, (_, i) => ({
    hour: i,
    commitCount: i === 10 ? 5 : 0
  }));

  const mockActivityDay: ActivityDistribution[] = [
    { hour: 0, commitCount: 2 },
    { hour: 1, commitCount: 3 },
    { hour: 2, commitCount: 1 },
    { hour: 3, commitCount: 2 },
    { hour: 4, commitCount: 1 },
    { hour: 5, commitCount: 1 },
    { hour: 6, commitCount: 0 }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getRepo',
      'syncRepo',
      'analyticsSummary',
      'analyticsTimeline',
      'analyticsActivityHour',
      'analyticsActivityDay',
      'stories',
      'createStory'
    ]);

    activatedRoute = {
      snapshot: {
        paramMap: {
          get: (key: string) => key === 'id' ? 'test-id' : null
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRoute }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load repo and analytics on init', () => {
    apiService.getRepo.and.returnValue(of(mockRepo));
    apiService.analyticsSummary.and.returnValue(of(mockSummary));
    apiService.analyticsTimeline.and.returnValue(of(mockTimeline));
    apiService.analyticsActivityHour.and.returnValue(of(mockActivityHour));
    apiService.analyticsActivityDay.and.returnValue(of(mockActivityDay));
    apiService.stories.and.returnValue(of([]));

    fixture.detectChanges();

    expect(apiService.getRepo).toHaveBeenCalledWith('test-id');
    expect(component.repo).toEqual(mockRepo);
    expect(component.summary).toEqual(mockSummary);
    expect(component.timeline.length).toBe(3);
  });

  it('should sync repository', () => {
    component.repo = mockRepo;
    apiService.syncRepo.and.returnValue(of({ commitsAdded: 5, message: 'Synced 5 commits' }));

    component.sync();

    expect(apiService.syncRepo).toHaveBeenCalledWith('test-id');
    expect(component.syncing).toBeFalsy();
  });

  it('should generate story with template mode', () => {
    const mockStory: Story = {
      id: 'story-1',
      repoId: 'test-id',
      title: 'Test Story',
      content: '# Story',
      mode: 'TEMPLATE',
      startSha: null,
      endSha: null,
      createdAt: new Date().toISOString()
    };

    component.repo = mockRepo;
    component.storyMode = 'TEMPLATE';
    component.storyTitle = 'Test Story';
    apiService.createStory.and.returnValue(of(mockStory));

    component.genStory();

    expect(apiService.createStory).toHaveBeenCalledWith('test-id', jasmine.objectContaining({
      mode: 'TEMPLATE',
      title: 'Test Story'
    }));
  });

  it('should calculate bar height correctly', () => {
    const point: TimelinePoint = { date: '2024-01-01', commitCount: 10 };
    component.timeline = [
      { date: '2024-01-01', commitCount: 10 },
      { date: '2024-01-02', commitCount: 5 }
    ];

    const height = component.barH(point);
    expect(height).toBe(100);
  });

  it('should calculate percentage correctly', () => {
    const pct = component.pct(5, 10);
    expect(pct).toBe(50);
  });

  it('should return day name for activity day', () => {
    const dayName = component.dayN(0);
    expect(dayName).toBe('Mon');
    
    const dayName1 = component.dayN(6);
    expect(dayName1).toBe('Sun');
  });

  it('should calculate max author commits', () => {
    component.summary = mockSummary;
    const max = component.maxAuthorCommits();
    expect(max).toBe(6);
  });

  it('should calculate max file changes', () => {
    component.summary = mockSummary;
    const max = component.maxFileChanges();
    expect(max).toBe(15);
  });

  it('should load stories on init', () => {
    const mockStories: Story[] = [
      { id: '1', repoId: 'test-id', title: 'Story 1', content: '# S1', mode: 'TEMPLATE', startSha: null, endSha: null, createdAt: new Date().toISOString() },
      { id: '2', repoId: 'test-id', title: 'Story 2', content: '# S2', mode: 'LLM', startSha: null, endSha: null, createdAt: new Date().toISOString() }
    ];

    apiService.getRepo.and.returnValue(of(mockRepo));
    apiService.analyticsSummary.and.returnValue(of(mockSummary));
    apiService.analyticsTimeline.and.returnValue(of([]));
    apiService.analyticsActivityHour.and.returnValue(of([]));
    apiService.analyticsActivityDay.and.returnValue(of([]));
    apiService.stories.and.returnValue(of(mockStories));

    fixture.detectChanges();

    expect(apiService.stories).toHaveBeenCalledWith('test-id');
    expect(component.stories.length).toBe(2);
  });

  it('should handle empty analytics data', () => {
    apiService.getRepo.and.returnValue(of(mockRepo));
    apiService.analyticsSummary.and.returnValue(of({
      totalCommits: 0,
      totalAuthors: 0,
      totalFilesChanged: 0,
      firstCommit: null,
      lastCommit: null,
      topAuthors: [],
      topFiles: []
    }));
    apiService.analyticsTimeline.and.returnValue(of([]));
    apiService.analyticsActivityHour.and.returnValue(of([]));
    apiService.analyticsActivityDay.and.returnValue(of([]));
    apiService.stories.and.returnValue(of([]));

    fixture.detectChanges();

    expect(component.summary?.totalCommits).toBe(0);
    expect(component.timeline.length).toBe(0);
  });
});
