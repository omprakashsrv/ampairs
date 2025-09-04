import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';

import { WorkspaceInvitationService } from '../../../../core/services/workspace-invitation.service';
import { WorkspaceMemberRole, getRoleColor, getRoleDescription } from '../../../../core/models/workspace-member.interface';

export interface CreateInvitationDialogData {
  workspaceId: string;
  currentUserRole: WorkspaceMemberRole;
}

export interface CreateInvitationDialogResult {
  created: boolean;
  count?: number;
}

export interface InvitationFormData {
  email: string;
  phone?: string;
  name?: string;
  role: WorkspaceMemberRole;
}

@Component({
  selector: 'app-create-invitation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatCheckboxModule,
    MatDividerModule,
    MatCardModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatExpansionModule
  ],
  templateUrl: './create-invitation-dialog.component.html',
  styleUrls: ['./create-invitation-dialog.component.scss']
})
export class CreateInvitationDialogComponent implements OnInit {
  invitationForm!: FormGroup;
  availableRoles: WorkspaceMemberRole[] = ['ADMIN', 'MANAGER', 'MEMBER', 'GUEST', 'VIEWER'];
  
  isLoading = false;
  isBulkMode = false;
  maxInvitations = 10;

  constructor(
    private dialogRef: MatDialogRef<CreateInvitationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateInvitationDialogData,
    private formBuilder: FormBuilder,
    private invitationService: WorkspaceInvitationService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.filterAvailableRoles();
    this.initializeForm();
  }

  private filterAvailableRoles(): void {
    // Filter roles based on current user's permissions
    if (this.data.currentUserRole === 'MANAGER') {
      this.availableRoles = ['MEMBER', 'GUEST', 'VIEWER'];
    } else if (this.data.currentUserRole === 'MEMBER') {
      this.availableRoles = ['GUEST', 'VIEWER'];
    }
  }

  private initializeForm(): void {
    this.invitationForm = this.formBuilder.group({
      customMessage: [''],
      notifyInvitees: [true],
      invitations: this.formBuilder.array([
        this.createInvitationFormGroup()
      ])
    });
  }

  private createInvitationFormGroup(): FormGroup {
    return this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      name: [''],
      role: [this.getDefaultRole(), Validators.required]
    });
  }

  private getDefaultRole(): WorkspaceMemberRole {
    // Default to the lowest available role
    return this.availableRoles[this.availableRoles.length - 1] || 'VIEWER';
  }

  get invitations(): FormArray {
    return this.invitationForm.get('invitations') as FormArray;
  }

  addInvitation(): void {
    if (this.invitations.length < this.maxInvitations) {
      this.invitations.push(this.createInvitationFormGroup());
    }
  }

  removeInvitation(index: number): void {
    if (this.invitations.length > 1) {
      this.invitations.removeAt(index);
    }
  }

  toggleBulkMode(): void {
    this.isBulkMode = !this.isBulkMode;
    if (this.isBulkMode && this.invitations.length === 1) {
      // Add a few more invitation fields for bulk mode
      this.addInvitation();
      this.addInvitation();
    }
  }

  getRoleColor(role: WorkspaceMemberRole): string {
    return getRoleColor(role);
  }

  getRoleDescription(role: WorkspaceMemberRole): string {
    return getRoleDescription(role);
  }

  isFormValid(): boolean {
    if (!this.invitationForm.valid) return false;
    
    // Check if at least one invitation has a valid email
    return this.invitations.controls.some(control => 
      control.get('email')?.valid && control.get('email')?.value?.trim()
    );
  }

  getValidInvitations(): InvitationFormData[] {
    return this.invitations.controls
      .map(control => control.value as InvitationFormData)
      .filter(invitation => 
        invitation.email && 
        invitation.email.trim() && 
        invitation.role
      );
  }

  onCancel(): void {
    this.dialogRef.close({ created: false });
  }

  onSend(): void {
    if (!this.isFormValid()) return;

    this.isLoading = true;
    const formValue = this.invitationForm.value;
    const validInvitations = this.getValidInvitations();

    if (validInvitations.length === 0) {
      this.showError('Please provide at least one valid email address');
      this.isLoading = false;
      return;
    }

    // Prepare bulk invitation request
    const bulkInvitationRequest = {
      invitations: validInvitations.map(invitation => ({
        email: invitation.email.trim(),
        phone: invitation.phone?.trim() || null,
        name: invitation.name?.trim() || null,
        role: invitation.role,
        custom_message: formValue.customMessage?.trim() || null,
        notify_invitee: formValue.notifyInvitees
      })),
      workspace_id: this.data.workspaceId
    };

    this.invitationService.sendBulkInvitations(bulkInvitationRequest)
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          const successCount = response.successful_invitations?.length || validInvitations.length;
          const failureCount = response.failed_invitations?.length || 0;
          
          if (successCount > 0) {
            this.showSuccess(`${successCount} invitation${successCount > 1 ? 's' : ''} sent successfully`);
          }
          
          if (failureCount > 0) {
            this.showWarning(`${failureCount} invitation${failureCount > 1 ? 's' : ''} failed to send`);
          }
          
          this.dialogRef.close({ 
            created: true, 
            count: successCount 
          });
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Failed to send invitations:', error);
          this.showError('Failed to send invitations. Please try again.');
        }
      });
  }

  // Utility methods for pre-filling common scenarios
  addCommonRoles(): void {
    const commonRoles: WorkspaceMemberRole[] = ['MEMBER', 'GUEST'];
    
    commonRoles.forEach(role => {
      if (this.availableRoles.includes(role) && this.invitations.length < this.maxInvitations) {
        const invitation = this.createInvitationFormGroup();
        invitation.patchValue({ role });
        this.invitations.push(invitation);
      }
    });
  }

  fillSampleData(): void {
    // For development/testing - fill with sample data
    if (this.invitations.length > 0) {
      this.invitations.at(0).patchValue({
        email: 'john.doe@example.com',
        name: 'John Doe',
        phone: '+1234567890',
        role: 'MEMBER'
      });
    }
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 6000,
      panelClass: ['error-snackbar']
    });
  }

  private showWarning(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['warning-snackbar']
    });
  }
}