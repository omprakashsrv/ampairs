import {Component, Inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatIconModule} from '@angular/material/icon';
import {MatChipsModule} from '@angular/material/chips';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';

import {MemberService} from '../../../core/services/member.service';
import {
  ROLE_DISPLAY_NAMES,
  WorkspaceMemberListItem,
  WorkspaceRole,
  MemberStatus
} from '../../../core/models/member.interface';
import {WorkspaceListItem} from '../../../core/services/workspace.service';

export interface MemberEditDialogData {
  member: WorkspaceMemberListItem;
  currentWorkspace: WorkspaceListItem;
}

@Component({
  selector: 'app-member-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule
  ],
  templateUrl: './member-edit-dialog.component.html',
  styleUrl: './member-edit-dialog.component.scss'
})
export class MemberEditDialogComponent implements OnInit {
  editForm: FormGroup;
  isLoading = signal(false);
  
  // Enums for template
  WorkspaceRole = WorkspaceRole;
  MemberStatus = MemberStatus;
  ROLE_DISPLAY_NAMES = ROLE_DISPLAY_NAMES;

  constructor(
    private fb: FormBuilder,
    private memberService: MemberService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<MemberEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MemberEditDialogData
  ) {
    this.editForm = this.fb.group({
      role: [data.member.role, Validators.required],
      is_active: [data.member.is_active, Validators.required]
    });
  }

  ngOnInit(): void {
    // Debug: Log the current member data to see what we're working with
    console.log('Member data:', this.data.member);
    console.log('Current role:', this.data.member.role);
    console.log('Available roles:', this.availableRoles);
    console.log('Form value:', this.editForm.value);
    
    // Ensure the form has the correct initial values
    this.editForm.patchValue({
      role: this.data.member.role,
      is_active: this.data.member.is_active
    });
  }

  get availableRoles(): WorkspaceRole[] {
    // Define available roles based on current user's role
    // Include all roles including OWNER for now - proper permissions can be added later
    return [
      WorkspaceRole.OWNER,
      WorkspaceRole.ADMIN,
      WorkspaceRole.MANAGER,
      WorkspaceRole.MEMBER,
      WorkspaceRole.GUEST,
      WorkspaceRole.VIEWER
    ];
  }

  get availableStatuses(): { value: boolean; label: string }[] {
    return [
      { value: true, label: 'Active' },
      { value: false, label: 'Inactive' }
    ];
  }

  async onSubmit(): Promise<void> {
    if (this.editForm.invalid) {
      return;
    }

    this.isLoading.set(true);

    try {
      const formValues = this.editForm.value;
      
      const updateRequest = {
        role: formValues.role,
        is_active: formValues.is_active
      };

      await this.memberService.updateMember(
        this.data.currentWorkspace.id,
        this.data.member.id,
        updateRequest
      );

      this.showSuccess('Member updated successfully');
      this.dialogRef.close(true);
    } catch (error: any) {
      this.showError(error.message || 'Failed to update member');
    } finally {
      this.isLoading.set(false);
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
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