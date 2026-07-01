import { Routes } from '@angular/router';
import { ReposComponent } from './pages/repos/repos.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { StoryViewComponent } from './pages/story-view/story-view.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: '', component: ReposComponent, canActivate: [AuthGuard] },
  { path: 'repo/:id', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'stories/:id', component: StoryViewComponent, canActivate: [AuthGuard] },
];
