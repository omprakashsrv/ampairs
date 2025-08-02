import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { RecaptchaService } from '../../core/services/recaptcha.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    private recaptchaService: RecaptchaService
  ) {
    this.loginForm = this.fb.group({
      mobileNumber: ['', [
        Validators.required,
        Validators.pattern(/^[6-9]\d{9}$/), // Indian mobile number pattern
        Validators.minLength(10),
        Validators.maxLength(10)
      ]]
    });
  }

  ngOnInit(): void {
    // Check if user is already authenticated
    this.authService.isAuthenticated$.subscribe(isAuthenticated => {
      if (isAuthenticated) {
        this.router.navigate(['/home']);
      }
    });
  }

  async onSubmit(): Promise<void> {
    if (this.loginForm.valid && !this.isLoading) {
      this.isLoading = true;
      const mobileNumber = this.loginForm.get('mobileNumber')?.value;

      try {
        // Get reCAPTCHA token first
        const recaptchaToken = await this.recaptchaService.getLoginToken();
        
        this.authService.initAuth(mobileNumber, recaptchaToken).subscribe({
          next: (response) => {
            this.isLoading = false;
            if (response.success && response.sessionId) {
              // Store session ID for OTP verification
              sessionStorage.setItem('auth_session_id', response.sessionId);
              sessionStorage.setItem('mobile_number', mobileNumber);
              
              this.snackBar.open('OTP sent successfully!', 'Close', {
                duration: 3000,
                panelClass: ['success-snackbar']
              });
              
              // Navigate to OTP verification page
              this.router.navigate(['/verify-otp']);
            } else {
              this.showError(response.error?.message || 'Failed to send OTP');
            }
          },
          error: (error) => {
            this.isLoading = false;
            this.showError(error.message || 'Failed to send OTP. Please try again.');
          }
        });
      } catch (recaptchaError) {
        console.error('reCAPTCHA error:', recaptchaError);
        this.isLoading = false;
        this.showError('Security verification failed. Please try again.');
      }
    }
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}