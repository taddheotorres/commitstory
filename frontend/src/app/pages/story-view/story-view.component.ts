import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Story } from '../../models/story.model';

@Component({
  selector: 'app-story-view',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="container" *ngIf="story">
      <a [routerLink]="['/repo', story.repoId]" class="back">&larr; Back</a>
      <article class="body"><div [innerHTML]="html"></div></article>
    </div>
  `,
  styles: [`
    .container { max-width: 800px; margin: 0 auto; padding: 2rem; }
    .back { text-decoration: none; color: #007bff; font-size: 0.9rem; display: inline-block; margin-bottom: 1rem; }
    .body { background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 2rem; line-height: 1.8; }
    .body h1 { border-bottom: 2px solid #333; padding-bottom: 0.5rem; }
    .body h2 { margin-top: 1.5rem; color: #444; }
    .body code { background: #f4f4f4; padding: 0.1rem 0.4rem; border-radius: 3px; font-size: 0.9rem; }
    .body pre { background: #f8f8f8; padding: 1rem; border-radius: 6px; overflow-x: auto; }
  `]
})
export class StoryViewComponent implements OnInit {
  story?: Story;
  html = '';

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.getStory(id).subscribe((s: Story) => {
      this.story = s;
      this.html = this.render(s.content);
    });
  }

  private render(md: string): string {
    return md.split('\n').map((l: string) => {
      if (l.startsWith('### ')) return '<h3>' + l.slice(4) + '</h3>';
      if (l.startsWith('## ')) return '<h2>' + l.slice(3) + '</h2>';
      if (l.startsWith('# ')) return '<h1>' + l.slice(2) + '</h1>';
      if (l.startsWith('- ') || l.startsWith('* ')) return '<li>' + l.slice(2) + '</li>';
      if (l.trim() === '') return '<br>';
      return '<p>' + l + '</p>';
    }).join('\n')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  }
}
