import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { DeviceManagementComponent } from '../../shared/components/device-management/device-management.component';

@Component({
  selector: 'app-devices',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    DeviceManagementComponent
  ],
  templateUrl: './devices.component.html',
  styleUrl: './devices.component.scss'
})
export class DevicesComponent {
  constructor(private router: Router) {}

  goBack(): void {
    this.router.navigate(['/home']);
  }
}