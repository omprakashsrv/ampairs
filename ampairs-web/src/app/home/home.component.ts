import {Component, OnInit} from '@angular/core';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {CommonModule} from '@angular/common';
import {AuthService, User} from '../core/services/auth.service';
import {DeviceManagementComponent} from '../shared/components/device-management/device-management.component';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatCardModule,
    MatDividerModule,
    DeviceManagementComponent
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  currentUser$: Observable<User | null>;
  showDeviceManagement = false;

  features = [
    {
      icon: 'security',
      title: 'Secure Authentication',
      description: 'Your data is protected with industry-standard security measures'
    },
    {
      icon: 'speed',
      title: 'Fast Performance',
      description: 'Optimized for speed and responsive user experience'
    },
    {
      icon: 'cloud',
      title: 'Cloud Integration',
      description: 'Seamlessly integrated with cloud services for reliability'
    },
    {
      icon: 'support',
      title: '24/7 Support',
      description: 'Round-the-clock customer support for all your needs'
    }
  ];

  constructor(private authService: AuthService) {
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    // Component initialization
  }

  editProfile(): void {
    // TODO: Implement profile editing
    console.log('Edit profile clicked');
  }

  viewSettings(): void {
    // TODO: Implement settings page
    console.log('Settings clicked');
  }

  viewDevices(): void {
    this.showDeviceManagement = !this.showDeviceManagement;
  }

  logout(): void {
    this.authService.logout();
  }
}
