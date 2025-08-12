import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

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
    path: 'home',
    loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'devices',
    loadComponent: () => import('./pages/devices/devices.component').then(m => m.DevicesComponent),
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];