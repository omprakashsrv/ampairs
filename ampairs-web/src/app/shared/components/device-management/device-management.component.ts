import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatChipsModule} from '@angular/material/chips';
import {MatTooltipModule} from '@angular/material/tooltip';
import {AuthService, DeviceSession} from '../../../core/services/auth.service';
import {DeviceService} from '../../../core/services/device.service';

@Component({
  selector: 'app-device-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatChipsModule,
    MatTooltipModule
  ],
  template: `
    <mat-card class="device-management-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>devices</mat-icon>
          Device Sessions
        </mat-card-title>
        <mat-card-subtitle>
          Manage your active login sessions across different devices
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <div *ngIf="isLoading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading device sessions...</p>
        </div>

        <div *ngIf="!isLoading && deviceSessions.length === 0" class="no-devices">
          <mat-icon>device_unknown</mat-icon>
          <p>No active device sessions found</p>
        </div>

        <div *ngIf="!isLoading && deviceSessions.length > 0" class="devices-container">
          <div *ngFor="let device of deviceSessions"
               class="device-item"
               [class.current-device]="device.is_current_device">

            <div class="device-header">
              <div class="device-icon">
                <mat-icon>{{ getDeviceIcon(device.device_type) }}</mat-icon>
              </div>

              <div class="device-info">
                <h3 class="device-name">{{ device.device_name }}</h3>
                <div class="device-details">
                  <mat-chip-set>
                    <mat-chip *ngIf="device.is_current_device" color="accent">Current Device</mat-chip>
                    <mat-chip>{{ device.platform }}</mat-chip>
                    <mat-chip>{{ device.browser }}</mat-chip>
                  </mat-chip-set>
                </div>
              </div>

              <div class="device-actions" *ngIf="!device.is_current_device">
                <button mat-icon-button
                        color="warn"
                        (click)="logoutDevice(device.device_id)"
                        matTooltip="Logout from this device"
                        [disabled]="isLoggingOut">
                  <mat-icon>logout</mat-icon>
                </button>
              </div>
            </div>

            <div class="device-metadata">
              <div class="metadata-item">
                <mat-icon>schedule</mat-icon>
                <span>Last activity: {{ formatDate(device.last_activity) }}</span>
              </div>

              <div class="metadata-item">
                <mat-icon>login</mat-icon>
                <span>Login time: {{ formatDate(device.login_time) }}</span>
              </div>

              <div class="metadata-item">
                <mat-icon>location_on</mat-icon>
                <span>IP: {{ device.ip_address }}</span>
                <span *ngIf="device.location"> • {{ device.location }}</span>
              </div>

              <div class="metadata-item">
                <mat-icon>computer</mat-icon>
                <span>{{ device.os }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="actions-container" *ngIf="deviceSessions.length > 1">
          <button mat-raised-button
                  color="warn"
                  (click)="logoutAllDevices()"
                  [disabled]="isLoggingOut">
            <mat-icon>logout</mat-icon>
            Logout All Devices
          </button>
        </div>
      </mat-card-content>

      <mat-card-actions>
        <button mat-button (click)="refreshDevices()" [disabled]="isLoading">
          <mat-icon>refresh</mat-icon>
          Refresh
        </button>

        <button mat-button (click)="showDebugInfo()" [disabled]="isLoading">
          <mat-icon>info</mat-icon>
          Device Debug Info
        </button>
      </mat-card-actions>
    </mat-card>

    <!-- Debug Info Dialog -->
    <div *ngIf="showDebugModal" class="debug-modal-overlay" (click)="closeDebugInfo()">
      <div class="debug-modal" (click)="$event.stopPropagation()">
        <div class="debug-header">
          <h3>Current Device Debug Information</h3>
          <button mat-icon-button (click)="closeDebugInfo()">
            <mat-icon>close</mat-icon>
          </button>
        </div>
        <div class="debug-content">
          <pre>{{ debugInfo | json }}</pre>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .device-management-card {
      max-width: 800px;
      margin: 20px auto;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px;
      gap: 16px;
    }

    .no-devices {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px;
      gap: 16px;
      color: rgba(0, 0, 0, 0.6);
    }

    .no-devices mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }

    .devices-container {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .device-item {
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 8px;
      padding: 16px;
      transition: all 0.2s ease-in-out;
    }

    .device-item:hover {
      border-color: rgba(0, 0, 0, 0.2);
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .device-item.current-device {
      border-color: #3f51b5;
      background-color: rgba(63, 81, 181, 0.05);
    }

    .device-header {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      margin-bottom: 12px;
    }

    .device-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      background-color: rgba(0, 0, 0, 0.08);
      border-radius: 50%;
    }

    .device-icon mat-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .device-info {
      flex: 1;
    }

    .device-name {
      margin: 0 0 8px 0;
      font-size: 16px;
      font-weight: 500;
    }

    .device-details mat-chip-set {
      gap: 8px;
    }

    .device-actions {
      display: flex;
      align-items: flex-start;
    }

    .device-metadata {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding-left: 64px;
    }

    .metadata-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: rgba(0, 0, 0, 0.7);
    }

    .metadata-item mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .actions-container {
      display: flex;
      justify-content: center;
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid rgba(0, 0, 0, 0.12);
    }

    .debug-modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .debug-modal {
      background: white;
      border-radius: 8px;
      max-width: 90vw;
      max-height: 90vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .debug-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.12);
      background-color: #f5f5f5;
    }

    .debug-header h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 500;
    }

    .debug-content {
      padding: 16px 24px;
      overflow: auto;
      flex: 1;
    }

    .debug-content pre {
      background-color: #f8f8f8;
      padding: 16px;
      border-radius: 4px;
      font-size: 12px;
      line-height: 1.4;
      white-space: pre-wrap;
      word-wrap: break-word;
      margin: 0;
    }

    @media (max-width: 768px) {
      .device-header {
        flex-direction: column;
        align-items: flex-start;
      }

      .device-metadata {
        padding-left: 0;
      }

      .device-actions {
        align-self: flex-end;
        margin-top: -40px;
      }

      .debug-modal {
        max-width: 95vw;
        max-height: 95vh;
      }

      .debug-content pre {
        font-size: 10px;
      }
    }
  `]
})
export class DeviceManagementComponent implements OnInit {
  deviceSessions: DeviceSession[] = [];
  isLoading = false;
  isLoggingOut = false;
  showDebugModal = false;
  debugInfo: any = null;

  constructor(
    private authService: AuthService,
    private deviceService: DeviceService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
  }

  ngOnInit(): void {
    this.loadDeviceSessions();
  }

  loadDeviceSessions(): void {
    this.isLoading = true;
    this.authService.getDeviceSessions().subscribe({
      next: (sessions) => {
        this.deviceSessions = sessions;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load device sessions:', error);
        this.showError('Failed to load device sessions');
        this.isLoading = false;
      }
    });
  }

  refreshDevices(): void {
    this.loadDeviceSessions();
  }

  logoutDevice(deviceId: string): void {
    const device = this.deviceSessions.find(d => d.device_id === deviceId);
    if (!device) return;

    if (confirm(`Are you sure you want to logout from "${device.device_name}"?`)) {
      this.isLoggingOut = true;
      this.authService.logoutDevice(deviceId).subscribe({
        next: () => {
          this.showSuccess(`Successfully logged out from ${device.device_name}`);
          this.loadDeviceSessions(); // Refresh the list
          this.isLoggingOut = false;
        },
        error: (error) => {
          console.error('Failed to logout device:', error);
          this.showError('Failed to logout from device');
          this.isLoggingOut = false;
        }
      });
    }
  }

  logoutAllDevices(): void {
    if (confirm('Are you sure you want to logout from all devices? This will end all your sessions and you will need to login again.')) {
      this.isLoggingOut = true;
      this.authService.logoutAllDevices().subscribe({
        next: () => {
          this.showSuccess('Successfully logged out from all devices');
          // User will be redirected to login page by the service
        },
        error: (error) => {
          console.error('Failed to logout all devices:', error);
          this.showError('Failed to logout from all devices');
          this.isLoggingOut = false;
        }
      });
    }
  }

  getDeviceIcon(deviceType: string): string {
    switch (deviceType.toLowerCase()) {
      case 'mobile':
        return 'smartphone';
      case 'tablet':
        return 'tablet';
      case 'desktop':
        return 'computer';
      default:
        return 'device_unknown';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMinutes < 1) {
      return 'Just now';
    } else if (diffMinutes < 60) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else if (diffHours < 24) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffDays < 7) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else {
      return date.toLocaleDateString();
    }
  }

  showDebugInfo(): void {
    this.debugInfo = {
      currentDeviceInfo: this.deviceService.getDeviceInfo(),
      platformDebugInfo: this.deviceService.getPlatformDebugInfo()
    };
    this.showDebugModal = true;
  }

  closeDebugInfo(): void {
    this.showDebugModal = false;
    this.debugInfo = null;
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
