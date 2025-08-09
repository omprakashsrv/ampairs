import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule
  ],
  template: `
  <div class="container" style="display:flex;justify-content:center;align-items:center;min-height:100vh;">
    <mat-card style="width: 100%; max-width: 420px;">
      <mat-card-title>Complete your profile</mat-card-title>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <mat-form-field appearance="outline" style="width:100%;margin-top:12px;">
            <mat-label>First name</mat-label>
            <input matInput formControlName="firstName" maxlength="100" />
          </mat-form-field>

          <mat-form-field appearance="outline" style="width:100%;margin-top:12px;">
            <mat-label>Last name</mat-label>
            <input matInput formControlName="lastName" maxlength="100" />
          </mat-form-field>

          <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || isSubmitting" style="margin-top:16px; width:100%;">
            {{ isSubmitting ? 'Saving...' : 'Save' }}
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  </div>
  `,
  styles: []
})
export class CompleteProfileComponent {
  form: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    const currentUser = this.authService.getCurrentUser();
    this.form = this.fb.group({
      firstName: [currentUser?.firstName || '', [Validators.required, Validators.maxLength(100)]],
      lastName: [currentUser?.lastName || '', [Validators.required, Validators.maxLength(100)]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting) return;
    this.isSubmitting = true;

    const { firstName, lastName } = this.form.value;
    this.authService.updateUserName(firstName.trim(), lastName.trim()).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.snackBar.open('Profile updated successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.isSubmitting = false;
        // Extract error message from interceptor-formatted error
        const errorMessage = err.error?.message || err.message || 'Failed to update profile';
        this.snackBar.open(errorMessage, 'Close', { duration: 4000 });
      }
    });
  }
}
