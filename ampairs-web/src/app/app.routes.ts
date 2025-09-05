import {Routes} from '@angular/router';
import {AuthGuard} from './core/guards/auth.guard';
import {WorkspaceGuard} from './core/guards/workspace.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  // Authentication routes (without main layout)
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'verify-otp',
    loadComponent: () => import('./auth/verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent)
  },
  // Authenticated routes (with main layout)
  {
    path: '',
    loadComponent: () => import('./shared/components/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [AuthGuard],
    children: [
      {
        path: 'complete-profile',
        loadComponent: () => import('./auth/complete-profile/complete-profile.component').then(m => m.CompleteProfileComponent)
      },
      {
        path: 'workspaces',
        loadComponent: () => import('./pages/workspace/workspace-select/workspace-select.component').then(m => m.WorkspaceSelectComponent)
      },
      {
        path: 'workspace/create',
        loadComponent: () => import('./pages/workspace/workspace-create/workspace-create.component').then(m => m.WorkspaceCreateComponent)
      },
      {
        path: 'home',
        loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
        canActivate: [WorkspaceGuard]
      },
      {
        path: 'w/:slug',
        loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
        canActivate: [WorkspaceGuard],
        children: [
          {
            path: '',
            redirectTo: 'dashboard',
            pathMatch: 'full'
          },
          {
            path: 'dashboard',
            loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
          },
          {
            path: 'profile',
            loadComponent: () => import('./auth/complete-profile/complete-profile.component').then(m => m.CompleteProfileComponent)
          },
          {
            path: 'devices',
            loadComponent: () => import('./pages/devices/devices.component').then(m => m.DevicesComponent)
          }
        ]
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
