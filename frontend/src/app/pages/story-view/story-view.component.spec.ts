import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoryViewComponent } from './story-view.component';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Story } from '../../models/story.model';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

describe('StoryViewComponent', () => {
  let component: StoryViewComponent;
  let fixture: ComponentFixture<StoryViewComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let activatedRoute: any;

  const mockStory: Story = {
    id: 'story-1',
    repoId: 'repo-1',
    title: 'The Journey of My Repo',
    content: '# The Journey\n## Chapter 1\nThis is a story about commits.\n\n- First commit\n- Second commit\n\n**Bold text** and `code`',
    mode: 'TEMPLATE',
    startSha: null,
    endSha: null,
    createdAt: new Date().toISOString()
  };

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', ['getStory']);

    activatedRoute = {
      snapshot: {
        paramMap: {
          get: (key: string) => key === 'id' ? 'story-1' : null
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [StoryViewComponent, RouterTestingModule],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRoute }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    fixture = TestBed.createComponent(StoryViewComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load story on init', () => {
    apiService.getStory.and.returnValue(of(mockStory));

    fixture.detectChanges();

    expect(apiService.getStory).toHaveBeenCalledWith('story-1');
    expect(component.story).toEqual(mockStory);
  });

  it('should render h1 heading', () => {
    const rendered = component['render']('# Title');
    expect(rendered).toContain('<h1>Title</h1>');
  });

  it('should render h2 heading', () => {
    const rendered = component['render']('## Subtitle');
    expect(rendered).toContain('<h2>Subtitle</h2>');
  });

  it('should render h3 heading', () => {
    const rendered = component['render']('### Subsubtitle');
    expect(rendered).toContain('<h3>Subsubtitle</h3>');
  });

  it('should render list items with dash', () => {
    const rendered = component['render']('- Item 1');
    expect(rendered).toContain('<li>Item 1</li>');
  });

  it('should render list items with asterisk', () => {
    const rendered = component['render']('* Item 1');
    expect(rendered).toContain('<li>Item 1</li>');
  });

  it('should render paragraphs', () => {
    const rendered = component['render']('This is a paragraph');
    expect(rendered).toContain('<p>This is a paragraph</p>');
  });

  it('should render inline code', () => {
    const rendered = component['render']('This has `code` in it');
    expect(rendered).toContain('<code>code</code>');
  });

  it('should render bold text', () => {
    const rendered = component['render']('This is **bold** text');
    expect(rendered).toContain('<strong>bold</strong>');
  });

  it('should render breaks for empty lines', () => {
    const rendered = component['render']('Line 1\n\nLine 2');
    expect(rendered).toContain('<br>');
  });

  it('should render complex markdown', () => {
    const markdown = '# Title\n## Section\nParagraph with **bold** and `code`\n- List item';
    const rendered = component['render'](markdown);
    
    expect(rendered).toContain('<h1>Title</h1>');
    expect(rendered).toContain('<h2>Section</h2>');
    expect(rendered).toContain('<strong>bold</strong>');
    expect(rendered).toContain('<code>code</code>');
    expect(rendered).toContain('<li>List item</li>');
  });

  it('should set html property when story is loaded', () => {
    apiService.getStory.and.returnValue(of(mockStory));

    fixture.detectChanges();

    expect(component.html).toBeTruthy();
    expect(component.html.length).toBeGreaterThan(0);
  });

  it('should handle API error gracefully', () => {
    apiService.getStory.and.returnValue(throwError(() => new Error('API Error')));
    spyOn(console, 'error');

    fixture.detectChanges();

    expect(console.error).toHaveBeenCalled();
  });

  it('should display story with empty content', () => {
    const emptyStory: Story = {
      ...mockStory,
      content: ''
    };
    apiService.getStory.and.returnValue(of(emptyStory));

    fixture.detectChanges();

    expect(component.story?.content).toBe('');
  });

  it('should handle multiple code blocks', () => {
    const markdown = 'Code: `first` and `second` here';
    const rendered = component['render'](markdown);
    
    const codeCount = (rendered.match(/<code>/g) || []).length;
    expect(codeCount).toBe(2);
  });

  it('should handle nested formatting', () => {
    const markdown = 'Text with **bold and `code`** mixed';
    const rendered = component['render'](markdown);
    
    expect(rendered).toContain('<strong>');
    expect(rendered).toContain('<code>');
  });
});
