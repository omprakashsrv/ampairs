import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {CommonModule} from '@angular/common';
import {interval, Subscription} from 'rxjs';
import {take} from 'rxjs/operators';
import {AuthService} from '../../core/services/auth.service';
import {ReCaptchaV3Service} from 'ng-recaptcha-2';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-verify-otp',
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
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss'
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  otpForm: FormGroup;
  isLoading = false;
  isResending = false;
  canResend = false;
  remainingTime = 30; // 30 seconds countdown
  maskedMobileNumber = '';
  private sessionId = '';
  private timerSubscription?: Subscription;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    private recaptchaV3Service: ReCaptchaV3Service
  ) {
    this.otpForm = this.fb.group({
      otp: ['', [
        Validators.required,
        Validators.pattern(/^\d{6}$/), // Exactly 6 digits
        Validators.minLength(6),
        Validators.maxLength(6)
      ]]
    });
  }

  ngOnInit(): void {
    // Get session data
    this.sessionId = sessionStorage.getItem('auth_session_id') || '';
    const mobileNumber = sessionStorage.getItem('mobile_number') || '';

    if (!this.sessionId || !mobileNumber) {
      this.snackBar.open('Session expired. Please try again.', 'Close', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
      this.router.navigate(['/login']);
      return;
    }

    // Mask mobile number for display
    this.maskedMobileNumber = this.maskMobileNumber(mobileNumber);

    // Start countdown timer
    this.startTimer();
  }

  ngOnDestroy(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
  }

  onOtpInput(event: any): void {
    // Auto-submit when 6 digits are entered
    const value = event.target.value;
    if (value.length === 6 && /^\d{6}$/.test(value)) {
      setTimeout(() => {
        if (this.otpForm.valid) {
          this.onSubmit();
        }
      }, 100);
    }
  }

  async onSubmit(): Promise<void> {
    if (this.otpForm.valid && !this.isLoading) {
      this.isLoading = true;
      const otp = this.otpForm.get('otp')?.value;

      // Check if reCAPTCHA is enabled for this environment
      if (!environment.recaptcha.enabled) {
        console.log('reCAPTCHA disabled for development, using dummy token for OTP verification');
        const dummyToken = 'dev-dummy-token-verify-' + Date.now();
        this.handleOtpVerification(otp, dummyToken);
        return;
      }

      // Get reCAPTCHA token using ng-recaptcha-2
      this.recaptchaV3Service.execute('verify_otp').subscribe({
        next: (recaptchaToken: string) => {
          console.log('Received reCAPTCHA token for OTP verification:', recaptchaToken);
          this.handleOtpVerification(otp, recaptchaToken);
        },
        error: (recaptchaError) => {
          console.error('reCAPTCHA error:', recaptchaError);
          this.isLoading = false;
          this.showError('Security verification failed. Please try again.');
        }
      });
    }
  }

  resendOtp(): void {
    if (!this.canResend || this.isResending) return;

    this.isResending = true;
    const mobileNumber = sessionStorage.getItem('mobile_number') || '';

    // Check if reCAPTCHA is enabled for this environment
    if (!environment.recaptcha.enabled) {
      console.log('reCAPTCHA disabled for development, using dummy token for resend OTP');
      const dummyToken = 'dev-dummy-token-resend-' + Date.now();
      this.handleResendOtp(mobileNumber, dummyToken);
      return;
    }

    // Get reCAPTCHA token using ng-recaptcha-2
    this.recaptchaV3Service.execute('resend_otp').subscribe({
      next: (recaptchaToken: string) => {
        console.log('Received reCAPTCHA token for resend OTP:', recaptchaToken);
        this.handleResendOtp(mobileNumber, recaptchaToken);
      },
      error: (recaptchaError) => {
        console.error('reCAPTCHA error:', recaptchaError);
        this.isResending = false;
        this.showError('Security verification failed. Please try again.');
      }
    });
  }

  private handleOtpVerification(otp: string, recaptchaToken: string): void {
    this.authService.verifyOtp(this.sessionId, otp, recaptchaToken).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.access_token && response.refresh_token) {
          // Clear session storage
          sessionStorage.removeItem('auth_session_id');
          sessionStorage.removeItem('mobile_number');

          this.snackBar.open('Login successful!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });

          // Navigate to home page
          this.router.navigate(['/home']);
        } else {
          this.showError('Invalid OTP. Please try again.');
          this.otpForm.get('otp')?.setValue('');
        }
      },
      error: (error) => {
        this.isLoading = false;
        this.showError(error.message || 'Failed to verify OTP. Please try again.');
        this.otpForm.get('otp')?.setValue('');
      }
    });
  }

  private handleResendOtp(mobileNumber: string, recaptchaToken: string): void {
    this.authService.initAuth(mobileNumber, recaptchaToken).subscribe({
      next: (response) => {
        this.isResending = false;
        if (response.success && response.session_id) {
          // Update session ID
          this.sessionId = response.session_id;
          sessionStorage.setItem('auth_session_id', response.session_id);

          this.snackBar.open('OTP resent successfully!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });

          // Reset timer
          this.canResend = false;
          this.remainingTime = 30;
          this.startTimer();
          this.otpForm.get('otp')?.setValue('');
        } else {
          this.showError(response.error?.message || 'Failed to resend OTP');
        }
      },
      error: (error) => {
        this.isResending = false;
        this.showError(error.message || 'Failed to resend OTP. Please try again.');
      }
    });
  }

  goBack(): void {
    // Clear session storage
    sessionStorage.removeItem('auth_session_id');
    sessionStorage.removeItem('mobile_number');
    this.router.navigate(['/login']);
  }

  private startTimer(): void {
    this.timerSubscription = interval(1000)
      .pipe(take(this.remainingTime))
      .subscribe({
        next: () => {
          this.remainingTime--;
        },
        complete: () => {
          this.canResend = true;
        }
      });
  }

  private maskMobileNumber(mobileNumber: string): string {
    if (mobileNumber.length !== 10) return mobileNumber;
    return `${mobileNumber.substring(0, 2)}******${mobileNumber.substring(8)}`;
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
