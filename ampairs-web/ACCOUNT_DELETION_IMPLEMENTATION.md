# Account Deletion Implementation Guide - Angular Web App

## Overview

This guide provides step-by-step instructions for implementing the account deletion feature in the Ampairs Angular web application using Material Design 3 components.

---

## 📋 Prerequisites

- Angular Material 3 installed
- HTTP client configured
- Authentication service with JWT tokens
- Router configured

---

## 🎨 UI Components

### 1. Settings Navigation Menu

**File:** `src/app/settings/settings.component.ts`

```typescript
import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent {
  settingsMenuItems = [
    {
      title: 'Profile',
      icon: 'person',
      route: '/settings/profile'
    },
    {
      title: 'Account',
      icon: 'manage_accounts',
      route: '/settings/account'
    },
    {
      title: 'Security',
      icon: 'security',
      route: '/settings/security'
    },
    {
      title: 'Notifications',
      icon: 'notifications',
      route: '/settings/notifications'
    },
    {
      title: 'Delete Account',
      icon: 'delete_forever',
      route: '/settings/delete-account',
      color: 'warn' // Red color for danger action
    }
  ];

  constructor(private router: Router) {}

  navigateTo(route: string) {
    this.router.navigate([route]);
  }
}
```

**Template:** `src/app/settings/settings.component.html`

```html
<mat-sidenav-container class="settings-container">
  <!-- Side Navigation -->
  <mat-sidenav mode="side" opened class="settings-sidenav">
    <mat-nav-list>
      <mat-list-item
        *ngFor="let item of settingsMenuItems"
        [routerLink]="item.route"
        routerLinkActive="active"
        [class.warn-item]="item.color === 'warn'">
        <mat-icon matListItemIcon [color]="item.color">{{ item.icon }}</mat-icon>
        <span matListItemTitle>{{ item.title }}</span>
      </mat-list-item>
    </mat-nav-list>
  </mat-sidenav>

  <!-- Main Content -->
  <mat-sidenav-content class="settings-content">
    <router-outlet></router-outlet>
  </mat-sidenav-content>
</mat-sidenav-container>
```

**Styles:** `src/app/settings/settings.component.scss`

```scss
.settings-container {
  height: 100vh;
}

.settings-sidenav {
  width: 280px;
  padding: 16px 0;
}

.settings-content {
  padding: 24px;
  background-color: #fafafa;
}

.warn-item {
  color: var(--mdc-theme-error, #b00020);

  mat-icon {
    color: var(--mdc-theme-error, #b00020);
  }
}

mat-list-item.active {
  background-color: rgba(103, 80, 164, 0.08);
  border-left: 4px solid var(--mdc-theme-primary);
}
```

---

### 2. Delete Account Component

**File:** `src/app/settings/delete-account/delete-account.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AccountDeletionService } from '../../services/account-deletion.service';
import { AuthService } from '../../services/auth.service';
import {
  AccountDeletionStatusResponse,
  WorkspaceOwnershipInfo
} from '../../models/account-deletion.model';
import { DeleteConfirmationDialogComponent } from './delete-confirmation-dialog/delete-confirmation-dialog.component';
import { BlockingWorkspacesDialogComponent } from './blocking-workspaces-dialog/blocking-workspaces-dialog.component';

@Component({
  selector: 'app-delete-account',
  templateUrl: './delete-account.component.html',
  styleUrls: ['./delete-account.component.scss']
})
export class DeleteAccountComponent implements OnInit {
  deletionStatus?: AccountDeletionStatusResponse;
  loading = false;
  deleteForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private accountDeletionService: AccountDeletionService,
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.deleteForm = this.fb.group({
      reason: ['', [Validators.maxLength(500)]]
    });
  }

  ngOnInit(): void {
    this.loadDeletionStatus();
  }

  loadDeletionStatus(): void {
    this.loading = true;
    this.accountDeletionService.getAccountDeletionStatus().subscribe({
      next: (response) => {
        this.deletionStatus = response.data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load deletion status:', error);
        this.loading = false;
      }
    });
  }

  openDeleteConfirmation(): void {
    const dialogRef = this.dialog.open(DeleteConfirmationDialogComponent, {
      width: '500px',
      data: {
        reason: this.deleteForm.get('reason')?.value
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.confirmed) {
        this.requestAccountDeletion(result.reason);
      }
    });
  }

  requestAccountDeletion(reason?: string): void {
    this.loading = true;
    this.accountDeletionService.requestAccountDeletion({
      confirmed: true,
      reason: reason || undefined
    }).subscribe({
      next: (response) => {
        if (response.data.deletionRequested) {
          // Success - show grace period info and logout
          this.showSuccessDialog(response.data.daysUntilPermanentDeletion || 30);
        } else if (response.data.blockingWorkspaces) {
          // Blocked - show workspaces
          this.showBlockingWorkspacesDialog(response.data.blockingWorkspaces);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to request account deletion:', error);
        this.loading = false;
      }
    });
  }

  cancelAccountDeletion(): void {
    const dialogRef = this.dialog.open(DeleteConfirmationDialogComponent, {
      width: '500px',
      data: {
        isCancel: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.confirmed) {
        this.executeCancelDeletion();
      }
    });
  }

  executeCancelDeletion(): void {
    this.loading = true;
    this.accountDeletionService.cancelAccountDeletion().subscribe({
      next: (response) => {
        this.loadDeletionStatus();
        this.loading = false;
        // Show success snackbar or message
      },
      error: (error) => {
        console.error('Failed to cancel account deletion:', error);
        this.loading = false;
      }
    });
  }

  private showSuccessDialog(daysUntilDeletion: number): void {
    // Show success message then logout
    setTimeout(() => {
      this.authService.logout();
      this.router.navigate(['/']);
    }, 3000);
  }

  private showBlockingWorkspacesDialog(workspaces: WorkspaceOwnershipInfo[]): void {
    this.dialog.open(BlockingWorkspacesDialogComponent, {
      width: '600px',
      data: { workspaces }
    });
  }
}
```

**Template:** `src/app/settings/delete-account/delete-account.component.html`

```html
<div class="delete-account-container">
  <mat-card>
    <mat-card-header>
      <mat-card-title>
        <mat-icon color="warn">delete_forever</mat-icon>
        Delete Account
      </mat-card-title>
    </mat-card-header>

    <mat-card-content>
      <!-- Loading State -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="50"></mat-spinner>
      </div>

      <!-- Active Account State -->
      <div *ngIf="!loading && !deletionStatus?.isDeleted" class="active-state">
        <mat-card appearance="outlined" class="warning-card">
          <mat-card-content>
            <div class="warning-header">
              <mat-icon color="warn">warning</mat-icon>
              <h3>Permanent Action</h3>
            </div>
            <p>
              Deleting your account will permanently remove all your personal data
              after a 30-day grace period. This action cannot be undone after the
              grace period expires.
            </p>
          </mat-card-content>
        </mat-card>

        <mat-card appearance="outlined" class="info-card">
          <mat-card-header>
            <mat-card-title>What will be deleted?</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <ul class="data-list">
              <li>
                <mat-icon color="accent">check_circle</mat-icon>
                Your profile information (name, email, phone)
              </li>
              <li>
                <mat-icon color="accent">check_circle</mat-icon>
                All authentication tokens and sessions
              </li>
              <li>
                <mat-icon color="accent">check_circle</mat-icon>
                Your workspace memberships (if not sole owner)
              </li>
              <li>
                <mat-icon color="accent">check_circle</mat-icon>
                Personal preferences and settings
              </li>
            </ul>
          </mat-card-content>
        </mat-card>

        <form [formGroup]="deleteForm" class="delete-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Reason for leaving (optional)</mat-label>
            <textarea
              matInput
              formControlName="reason"
              rows="4"
              maxlength="500"
              placeholder="Help us improve by sharing why you're leaving..."></textarea>
            <mat-hint align="end">
              {{ deleteForm.get('reason')?.value?.length || 0 }} / 500
            </mat-hint>
          </mat-form-field>

          <div class="actions">
            <button
              mat-raised-button
              color="warn"
              (click)="openDeleteConfirmation()"
              [disabled]="loading">
              <mat-icon>delete_forever</mat-icon>
              Delete My Account
            </button>
          </div>
        </form>
      </div>

      <!-- Pending Deletion State -->
      <div *ngIf="!loading && deletionStatus?.isDeleted" class="deletion-pending">
        <mat-card appearance="outlined" class="status-card">
          <mat-card-content>
            <div class="status-header">
              <mat-icon color="warn">schedule</mat-icon>
              <h3>Account Deletion Scheduled</h3>
            </div>

            <div class="countdown">
              <h2>{{ deletionStatus.daysRemaining }}</h2>
              <p>days remaining until permanent deletion</p>
            </div>

            <mat-divider></mat-divider>

            <div class="status-details">
              <p><strong>Deletion Requested:</strong> {{ deletionStatus.deletedAt | date:'medium' }}</p>
              <p><strong>Permanent Deletion Date:</strong> {{ deletionStatus.deletionScheduledFor | date:'medium' }}</p>
              <p *ngIf="deletionStatus.deletionReason">
                <strong>Reason:</strong> {{ deletionStatus.deletionReason }}
              </p>
            </div>

            <mat-card appearance="outlined" class="info-box" *ngIf="deletionStatus.canRestore">
              <mat-card-content>
                <mat-icon color="primary">info</mat-icon>
                <p>
                  You can still cancel this deletion request and restore your account
                  within the next {{ deletionStatus.daysRemaining }} days.
                </p>
              </mat-card-content>
            </mat-card>

            <div class="actions" *ngIf="deletionStatus.canRestore">
              <button
                mat-raised-button
                color="primary"
                (click)="cancelAccountDeletion()"
                [disabled]="loading">
                <mat-icon>restore</mat-icon>
                Restore My Account
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </mat-card-content>
  </mat-card>
</div>
```

**Styles:** `src/app/settings/delete-account/delete-account.component.scss`

```scss
.delete-account-container {
  max-width: 800px;
  margin: 0 auto;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.warning-card {
  background-color: #fff3e0;
  margin-bottom: 24px;

  .warning-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    h3 {
      margin: 0;
      color: #e65100;
    }
  }
}

.info-card {
  margin-bottom: 24px;

  .data-list {
    list-style: none;
    padding: 0;

    li {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 12px;

      mat-icon {
        margin-top: 2px;
      }
    }
  }
}

.delete-form {
  margin-top: 24px;

  .full-width {
    width: 100%;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 16px;
  }
}

.deletion-pending {
  .status-card {
    .status-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 24px;

      h3 {
        margin: 0;
        color: #c62828;
      }
    }

    .countdown {
      text-align: center;
      padding: 24px;
      background-color: #ffebee;
      border-radius: 8px;
      margin-bottom: 24px;

      h2 {
        font-size: 64px;
        margin: 0;
        color: #c62828;
      }

      p {
        margin: 8px 0 0;
        color: #666;
      }
    }

    .status-details {
      margin: 24px 0;

      p {
        margin: 12px 0;
      }
    }

    .info-box {
      background-color: #e3f2fd;
      margin: 24px 0;

      mat-card-content {
        display: flex;
        gap: 12px;
        align-items: flex-start;
      }
    }

    .actions {
      display: flex;
      justify-content: center;
      margin-top: 24px;
    }
  }
}

mat-card-header {
  mat-card-title {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}
```

---

### 3. Delete Confirmation Dialog

**File:** `src/app/settings/delete-account/delete-confirmation-dialog/delete-confirmation-dialog.component.ts`

```typescript
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface DialogData {
  reason?: string;
  isCancel?: boolean;
}

@Component({
  selector: 'app-delete-confirmation-dialog',
  templateUrl: './delete-confirmation-dialog.component.html',
  styleUrls: ['./delete-confirmation-dialog.component.scss']
})
export class DeleteConfirmationDialogComponent {
  confirmText = '';

  constructor(
    public dialogRef: MatDialogRef<DeleteConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {}

  get isConfirmEnabled(): boolean {
    if (this.data.isCancel) {
      return this.confirmText === 'RESTORE';
    }
    return this.confirmText === 'DELETE';
  }

  onConfirm(): void {
    this.dialogRef.close({
      confirmed: true,
      reason: this.data.reason
    });
  }

  onCancel(): void {
    this.dialogRef.close({ confirmed: false });
  }
}
```

**Template:** `src/app/settings/delete-account/delete-confirmation-dialog/delete-confirmation-dialog.component.html`

```html
<h2 mat-dialog-title>
  <mat-icon [color]="data.isCancel ? 'primary' : 'warn'">
    {{ data.isCancel ? 'restore' : 'warning' }}
  </mat-icon>
  {{ data.isCancel ? 'Restore Account' : 'Confirm Account Deletion' }}
</h2>

<mat-dialog-content>
  <div class="dialog-message">
    <p *ngIf="!data.isCancel">
      <strong>Are you absolutely sure?</strong>
    </p>
    <p *ngIf="!data.isCancel">
      This will schedule your account for permanent deletion in 30 days.
      During this grace period, you can restore your account. After 30 days,
      all your data will be permanently deleted and cannot be recovered.
    </p>

    <p *ngIf="data.isCancel">
      <strong>Restore your account?</strong>
    </p>
    <p *ngIf="data.isCancel">
      This will cancel the deletion request and reactivate your account immediately.
    </p>
  </div>

  <mat-form-field appearance="outline" class="full-width confirmation-field">
    <mat-label>
      Type "{{ data.isCancel ? 'RESTORE' : 'DELETE' }}" to confirm
    </mat-label>
    <input
      matInput
      [(ngModel)]="confirmText"
      [placeholder]="data.isCancel ? 'RESTORE' : 'DELETE'"
      autocomplete="off">
  </mat-form-field>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="onCancel()">Cancel</button>
  <button
    mat-raised-button
    [color]="data.isCancel ? 'primary' : 'warn'"
    (click)="onConfirm()"
    [disabled]="!isConfirmEnabled">
    {{ data.isCancel ? 'Restore Account' : 'Delete My Account' }}
  </button>
</mat-dialog-actions>
```

---

### 4. Blocking Workspaces Dialog

**File:** `src/app/settings/delete-account/blocking-workspaces-dialog/blocking-workspaces-dialog.component.ts`

```typescript
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { WorkspaceOwnershipInfo } from '../../../models/account-deletion.model';

export interface DialogData {
  workspaces: WorkspaceOwnershipInfo[];
}

@Component({
  selector: 'app-blocking-workspaces-dialog',
  templateUrl: './blocking-workspaces-dialog.component.html',
  styleUrls: ['./blocking-workspaces-dialog.component.scss']
})
export class BlockingWorkspacesDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<BlockingWorkspacesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData,
    private router: Router
  ) {}

  goToWorkspaces(): void {
    this.dialogRef.close();
    this.router.navigate(['/workspaces']);
  }

  close(): void {
    this.dialogRef.close();
  }
}
```

**Template:** `src/app/settings/delete-account/blocking-workspaces-dialog/blocking-workspaces-dialog.component.html`

```html
<h2 mat-dialog-title>
  <mat-icon color="warn">block</mat-icon>
  Cannot Delete Account
</h2>

<mat-dialog-content>
  <div class="dialog-message">
    <p>
      <strong>You are the sole owner of {{ data.workspaces.length }} workspace(s).</strong>
    </p>
    <p>
      Before deleting your account, you must either transfer ownership to another
      member or delete these workspaces:
    </p>
  </div>

  <mat-list class="workspaces-list">
    <mat-list-item *ngFor="let workspace of data.workspaces">
      <mat-icon matListItemIcon>business</mat-icon>
      <div matListItemTitle>{{ workspace.workspaceName }}</div>
      <div matListItemLine>
        <span class="workspace-slug">@{{ workspace.workspaceSlug }}</span>
        <span class="member-count">{{ workspace.memberCount }} members</span>
      </div>
    </mat-list-item>
  </mat-list>

  <mat-card appearance="outlined" class="info-box">
    <mat-card-content>
      <mat-icon color="primary">info</mat-icon>
      <div>
        <strong>What you can do:</strong>
        <ul>
          <li>Transfer ownership to another workspace member</li>
          <li>Delete the workspace entirely</li>
        </ul>
      </div>
    </mat-card-content>
  </mat-card>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="close()">Close</button>
  <button mat-raised-button color="primary" (click)="goToWorkspaces()">
    <mat-icon>arrow_forward</mat-icon>
    Manage Workspaces
  </button>
</mat-dialog-actions>
```

---

## 🔌 API Service

**File:** `src/app/services/account-deletion.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AccountDeletionRequest,
  AccountDeletionResponse,
  AccountDeletionStatusResponse,
  ApiResponse
} from '../models/account-deletion.model';

@Injectable({
  providedIn: 'root'
})
export class AccountDeletionService {
  private apiUrl = `${environment.apiBaseUrl}/api/v1/account`;

  constructor(private http: HttpClient) {}

  /**
   * Request account deletion
   */
  requestAccountDeletion(request: AccountDeletionRequest): Observable<ApiResponse<AccountDeletionResponse>> {
    return this.http.post<ApiResponse<AccountDeletionResponse>>(
      `${this.apiUrl}/delete-request`,
      request
    );
  }

  /**
   * Cancel account deletion (restore account)
   */
  cancelAccountDeletion(): Observable<ApiResponse<AccountDeletionResponse>> {
    return this.http.post<ApiResponse<AccountDeletionResponse>>(
      `${this.apiUrl}/delete-cancel`,
      {}
    );
  }

  /**
   * Get account deletion status
   */
  getAccountDeletionStatus(): Observable<ApiResponse<AccountDeletionStatusResponse>> {
    return this.http.get<ApiResponse<AccountDeletionStatusResponse>>(
      `${this.apiUrl}/delete-status`
    );
  }
}
```

---

## 📦 Models

**File:** `src/app/models/account-deletion.model.ts`

```typescript
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  timestamp: string;
  path?: string;
  traceId?: string;
}

export interface AccountDeletionRequest {
  reason?: string;
  confirmed: boolean;
}

export interface AccountDeletionResponse {
  userId: string;
  deletionRequested: boolean;
  deletedAt?: string;
  deletionScheduledFor?: string;
  daysUntilPermanentDeletion?: number;
  message: string;
  blockingWorkspaces?: WorkspaceOwnershipInfo[];
  canRestore: boolean;
}

export interface AccountDeletionStatusResponse {
  isDeleted: boolean;
  deletedAt?: string;
  deletionScheduledFor?: string;
  daysRemaining?: number;
  canRestore: boolean;
  deletionReason?: string;
  statusMessage: string;
}

export interface WorkspaceOwnershipInfo {
  workspaceId: string;
  workspaceName: string;
  workspaceSlug: string;
  memberCount: number;
}
```

---

## 🛣️ Routing

**File:** `src/app/app-routing.module.ts` (add routes)

```typescript
const routes: Routes = [
  // ... other routes
  {
    path: 'settings',
    component: SettingsComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'profile', component: ProfileComponent },
      { path: 'account', component: AccountComponent },
      { path: 'delete-account', component: DeleteAccountComponent },
      { path: '', redirectTo: 'profile', pathMatch: 'full' }
    ]
  }
];
```

---

## 🧪 Testing

### Unit Test Example

**File:** `src/app/settings/delete-account/delete-account.component.spec.ts`

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DeleteAccountComponent } from './delete-account.component';
import { AccountDeletionService } from '../../services/account-deletion.service';
import { of, throwError } from 'rxjs';

describe('DeleteAccountComponent', () => {
  let component: DeleteAccountComponent;
  let fixture: ComponentFixture<DeleteAccountComponent>;
  let accountDeletionService: jasmine.SpyObj<AccountDeletionService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('AccountDeletionService', [
      'getAccountDeletionStatus',
      'requestAccountDeletion',
      'cancelAccountDeletion'
    ]);

    await TestBed.configureTestingModule({
      declarations: [ DeleteAccountComponent ],
      providers: [
        { provide: AccountDeletionService, useValue: spy }
      ]
    }).compileComponents();

    accountDeletionService = TestBed.inject(AccountDeletionService) as jasmine.SpyObj<AccountDeletionService>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeleteAccountComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load deletion status on init', () => {
    const mockResponse = {
      success: true,
      data: {
        isDeleted: false,
        canRestore: false,
        statusMessage: 'Account is active'
      }
    };

    accountDeletionService.getAccountDeletionStatus.and.returnValue(of(mockResponse));

    component.ngOnInit();

    expect(accountDeletionService.getAccountDeletionStatus).toHaveBeenCalled();
    expect(component.deletionStatus).toEqual(mockResponse.data);
  });
});
```

---

## 📋 Deployment Checklist

- [ ] Install Angular Material if not already installed
- [ ] Create all component files
- [ ] Create service file
- [ ] Create model file
- [ ] Add routes to routing module
- [ ] Test API integration with backend
- [ ] Test all user flows (delete, cancel, blocking)
- [ ] Verify error handling
- [ ] Test responsive design on mobile
- [ ] Verify accessibility (keyboard navigation, screen readers)
- [ ] Add loading states and error messages
- [ ] Test with actual JWT authentication

---

## 🎯 User Flow Summary

1. **User navigates to Settings → Delete Account**
2. **System loads deletion status** (active or pending)
3. **If active:**
   - User enters optional reason
   - User clicks "Delete My Account"
   - Confirmation dialog appears
   - User types "DELETE" to confirm
   - API call to request deletion
   - If successful: logout and redirect
   - If blocked: show blocking workspaces dialog
4. **If pending deletion:**
   - Show countdown and details
   - User can click "Restore My Account"
   - Confirmation dialog appears
   - User types "RESTORE" to confirm
   - API call to cancel deletion
   - Account reactivated

---

**Implementation Status:** ✅ Ready to implement
**Estimated Time:** 4-6 hours
**Difficulty:** Medium
