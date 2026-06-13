import { Routes } from '@angular/router';
import { ReposComponent } from './pages/repos/repos.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { StoryViewComponent } from './pages/story-view/story-view.component';

export const routes: Routes = [
  { path: '', component: ReposComponent },
  { path: 'repo/:id', component: DashboardComponent },
  { path: 'stories/:id', component: StoryViewComponent },
];
