import {Routes} from '@angular/router';
import {AuthGuard} from './core/guards/auth.guard';
import {WorkspaceGuard} from './core/guards/workspace.guard';
import {WorkspaceMemberGuard} from './core/guards/workspace-member.guard';
import {WorkspaceInvitationGuard} from './core/guards/workspace-invitation.guard';

export const routes: Routes = [
    {
        path: '',
        redirectTo: '/login',
        pathMatch: 'full'
    },
    {
        path: 'login',
        loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
    },
    {
        path: 'verify-otp',
        loadComponent: () => import('./auth/verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent)
    },
    {
        path: 'complete-profile',
        loadComponent: () => import('./auth/complete-profile/complete-profile.component').then(m => m.CompleteProfileComponent),
        canActivate: [AuthGuard]
    },
    {
      path: 'workspaces',
      canActivate: [AuthGuard],
      loadComponent: () => import('./pages/workspace/workspace-select/workspace-select.component').then(m => m.WorkspaceSelectComponent)
    },
  {
    path: 'workspace/create',
    canActivate: [AuthGuard],
    loadComponent: () => import('./pages/workspace/workspace-create/workspace-create.component').then(m => m.WorkspaceCreateComponent)
  },
  {
    path: 'w/:slug',
        loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
    canActivate: [AuthGuard, WorkspaceGuard],
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
            },
            // Workspace Management Routes
            {
                path: 'members',
                loadComponent: () => import('./pages/workspace/workspace-members/workspace-members.component').then(m => m.WorkspaceMembersComponent),
                canActivate: [WorkspaceMemberGuard]
            },
            {
                path: 'invitations',
                loadComponent: () => import('./pages/workspace/workspace-invitations/workspace-invitations.component').then(m => m.WorkspaceInvitationsComponent),
                canActivate: [WorkspaceInvitationGuard]
            },
            {
                path: 'modules',
                loadComponent: () => import('./pages/workspace/workspace-modules/workspace-modules.component').then(m => m.WorkspaceModulesComponent),
                canActivate: [WorkspaceMemberGuard]
            }
        ]
    },
    {
        path: '**',
        redirectTo: '/login'
    }
];
