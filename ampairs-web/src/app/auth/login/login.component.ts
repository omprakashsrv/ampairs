import {Component, OnInit} from '@angular/core';
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
import {AuthService} from '../../core/services/auth.service';
import {ReCaptchaV3Service} from 'ng-recaptcha-2';
import {environment} from '../../../environments/environment';

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
    private recaptchaV3Service: ReCaptchaV3Service
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

    // Initialize reCAPTCHA by adding script to head if not already present
    this.initializeRecaptcha();
  }

  onSubmit(): void {
    if (this.loginForm.valid && !this.isLoading) {
      this.isLoading = true;
      const mobileNumber = this.loginForm.get('mobileNumber')?.value;

      // Check if reCAPTCHA is enabled for this environment
      if (!environment.recaptcha.enabled) {
        console.log('reCAPTCHA disabled for development, using dummy token');
        const dummyToken = 'dev-dummy-token-' + Date.now();
        this.handleAuthRequest(mobileNumber, dummyToken);
        return;
      }

      // Add a small delay to ensure reCAPTCHA is ready
      setTimeout(() => {
        // Get reCAPTCHA token using ng-recaptcha-2
        console.log('Attempting to execute reCAPTCHA...');
        this.recaptchaV3Service.execute('login').subscribe({
          next: (recaptchaToken: string) => {
            console.log('Received reCAPTCHA token:', recaptchaToken);
            console.log('Token length:', recaptchaToken ? recaptchaToken.length : 'null/undefined');

            if (!recaptchaToken) {
              console.error('reCAPTCHA token is null or empty');
              this.isLoading = false;
              this.showError('Security verification failed. Please try again.');
              return;
            }

            this.handleAuthRequest(mobileNumber, recaptchaToken);
          },
          error: (recaptchaError) => {
            console.error('reCAPTCHA error:', recaptchaError);
            this.isLoading = false;
            this.showError('Security verification failed. Please try again.');
          }
        });
      }, 1000); // 1 second delay to ensure reCAPTCHA is ready
    }
  }

  private initializeRecaptcha(): void {
    // Check if reCAPTCHA script is already loaded
    if (!document.querySelector('script[src*="recaptcha"]')) {
      console.log('Loading reCAPTCHA script manually...');
      const script = document.createElement('script');
      script.src = `https://www.google.com/recaptcha/api.js?render=6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI`;
      script.async = true;
      script.defer = true;
      script.onload = () => {
        console.log('reCAPTCHA script loaded successfully');
      };
      script.onerror = () => {
        console.error('Failed to load reCAPTCHA script');
      };
      document.head.appendChild(script);
    } else {
      console.log('reCAPTCHA script already loaded');
    }
  }

  private handleAuthRequest(mobileNumber: string, recaptchaToken: string): void {
    this.authService.initAuth(mobileNumber, recaptchaToken).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success && response.session_id) {
          // Store session ID for OTP verification
          sessionStorage.setItem('auth_session_id', response.session_id);
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
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
