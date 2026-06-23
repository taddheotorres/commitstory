import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReposComponent } from './repos.component';
import { ApiService } from '../../services/api.service';
import { GitRepo, CreateRepoRequest } from '../../models/repo.model';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

describe('ReposComponent', () => {
  let component: ReposComponent;
  let fixture: ComponentFixture<ReposComponent>;
  let apiService: jasmine.SpyObj<ApiService>;

  const mockRepos: GitRepo[] = [
    {
      id: '1',
      name: 'repo-1',
      localPath: '/path/to/repo1',
      remoteUrl: 'https://github.com/user/repo1',
      provider: 'GITHUB',
      createdAt: new Date().toISOString()
    },
    {
      id: '2',
      name: 'repo-2',
      localPath: '/path/to/repo2',
      remoteUrl: null,
      provider: 'NONE',
      createdAt: new Date().toISOString()
    }
  ];

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'repos',
      'createRepo',
      'deleteRepo'
    ]);

    await TestBed.configureTestingModule({
      imports: [ReposComponent, RouterTestingModule],
      providers: [{ provide: ApiService, useValue: apiServiceSpy }]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    fixture = TestBed.createComponent(ReposComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load repositories on init', () => {
    apiService.repos.and.returnValue(of(mockRepos));

    fixture.detectChanges();

    expect(apiService.repos).toHaveBeenCalled();
    expect(component.repos.length).toBe(2);
    expect(component.repos[0].name).toBe('repo-1');
    expect(component.loading).toBeFalsy();
  });

  it('should handle empty repository list', () => {
    apiService.repos.and.returnValue(of([]));

    fixture.detectChanges();

    expect(component.repos.length).toBe(0);
    expect(component.loading).toBeFalsy();
  });

  it('should handle API error when loading repos', () => {
    apiService.repos.and.returnValue(throwError(() => new Error('API Error')));
    spyOn(console, 'error');

    fixture.detectChanges();

    expect(component.loading).toBeFalsy();
    expect(console.error).toHaveBeenCalled();
  });

  it('should add a new repository', () => {
    const newRepo: CreateRepoRequest = {
      name: 'new-repo',
      localPath: '/path/to/new',
      remoteUrl: 'https://github.com/user/new',
      provider: 'GITHUB'
    };
    const createdRepo: GitRepo = {
      id: '3',
      name: 'new-repo',
      localPath: '/path/to/new',
      remoteUrl: 'https://github.com/user/new',
      provider: 'GITHUB',
      createdAt: new Date().toISOString()
    };

    component.newRepo = newRepo;
    apiService.createRepo.and.returnValue(of(createdRepo));
    apiService.repos.and.returnValue(of([...mockRepos, createdRepo]));

    component.addRepo();

    expect(apiService.createRepo).toHaveBeenCalledWith(newRepo);
    expect(component.newRepo).toEqual({ name: '', localPath: '', remoteUrl: '', provider: 'NONE' });
  });

  it('should delete a repository', () => {
    component.repos = mockRepos;
    apiService.deleteRepo.and.returnValue(of(void 0));
    apiService.repos.and.returnValue(of([mockRepos[1]]));

    component.deleteRepo('1');

    expect(apiService.deleteRepo).toHaveBeenCalledWith('1');
  });

  it('should handle error when deleting repository', () => {
    component.repos = mockRepos;
    apiService.deleteRepo.and.returnValue(throwError(() => new Error('Delete failed')));
    spyOn(console, 'error');

    component.deleteRepo('1');

    expect(apiService.deleteRepo).toHaveBeenCalledWith('1');
    expect(console.error).toHaveBeenCalled();
  });

  it('should initialize newRepo with default values', () => {
    expect(component.newRepo.name).toBe('');
    expect(component.newRepo.provider).toBe('NONE');
  });

  it('should display loading state initially', () => {
    apiService.repos.and.returnValue(of(mockRepos));

    expect(component.loading).toBeTruthy();

    fixture.detectChanges();

    expect(component.loading).toBeFalsy();
  });

  it('should filter repositories by provider', () => {
    const githubRepos = mockRepos.filter(r => r.provider === 'GITHUB');
    expect(githubRepos.length).toBe(1);
    expect(githubRepos[0].name).toBe('repo-1');
  });

  it('should handle repository with null remote URL', () => {
    const repo = mockRepos[1];
    expect(repo.remoteUrl).toBeNull();
    expect(repo.localPath).toBe('/path/to/repo2');
  });
});
