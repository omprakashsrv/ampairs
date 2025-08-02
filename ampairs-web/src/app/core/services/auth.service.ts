import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import Cookies from 'js-cookie';
import { environment } from '../../../environments/environment';
import { NotificationService } from './notification.service';
import { RecaptchaService } from './recaptcha.service';

export interface AuthInitRequest {
  phone: string;
  countryCode: number;
  tokenId?: string;
  recaptchaToken?: string;
}

export interface AuthInitResponse {
  success: boolean;
  sessionId?: string;
  error?: {
    code: string;
    message: string;
  };
}

export interface OtpVerificationRequest {
  sessionId: string;
  otp: string;
  authMode: string;
  recaptchaToken?: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token: string;
}

export interface User {
  id: string;
  mobileNumber: string;
  name?: string;
  email?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly AUTH_API_URL = `${environment.apiBaseUrl}/auth/v1`;
  private readonly USER_API_URL = `${environment.apiBaseUrl}/user/v1`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);

  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private notificationService: NotificationService,
    private recaptchaService: RecaptchaService
  ) {
    this.checkAuthenticationStatus();
  }

  /**
   * Initialize authentication by sending mobile number with reCAPTCHA
   */
  async initAuthWithRecaptcha(mobileNumber: string): Promise<Observable<AuthInitResponse>> {
    try {
      const recaptchaToken = await this.recaptchaService.getLoginToken();
      return this.initAuth(mobileNumber, recaptchaToken);
    } catch (error) {
      console.error('reCAPTCHA error:', error);
      // Fallback to without reCAPTCHA if it fails
      return this.initAuth(mobileNumber);
    }
  }

  /**
   * Initialize authentication by sending mobile number
   */
  initAuth(mobileNumber: string, recaptchaToken?: string): Observable<AuthInitResponse> {
    const request: AuthInitRequest = {
      phone: mobileNumber,
      countryCode: 91,
      tokenId: '',
      recaptchaToken: recaptchaToken
    };

    return this.http.post<AuthInitResponse>(`${this.AUTH_API_URL}/init`, request)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Verify OTP with reCAPTCHA
   */
  async verifyOtpWithRecaptcha(sessionId: string, otp: string): Promise<Observable<AuthResponse>> {
    try {
      const recaptchaToken = await this.recaptchaService.getVerifyOtpToken();
      return this.verifyOtp(sessionId, otp, recaptchaToken);
    } catch (error) {
      console.error('reCAPTCHA error:', error);
      // Fallback to without reCAPTCHA if it fails
      return this.verifyOtp(sessionId, otp);
    }
  }

  /**
   * Verify OTP and complete authentication
   */
  verifyOtp(sessionId: string, otp: string, recaptchaToken?: string): Observable<AuthResponse> {
    const request: OtpVerificationRequest = {
      sessionId: sessionId,
      otp: otp,
      authMode: 'OTP',
      recaptchaToken: recaptchaToken
    };

    return this.http.post<AuthResponse>(`${this.AUTH_API_URL}/verify`, request)
      .pipe(
        map(response => {
          if (response.access_token && response.refresh_token) {
            this.setAuthTokens(response.access_token, response.refresh_token);
            this.isAuthenticatedSubject.next(true);
            // Get user profile after successful authentication
            this.getUserProfile().subscribe({
              next: (user) => this.currentUserSubject.next(user),
              error: (error) => console.error('Failed to get user profile:', error)
            });
          }
          return response;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Refresh access token using refresh token
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.logout('No refresh token available');
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>(`${this.AUTH_API_URL}/refresh_token`, {
      refreshToken: refreshToken
    }).pipe(
      map(response => {
        if (response.access_token && response.refresh_token) {
          this.setAuthTokens(response.access_token, response.refresh_token);
          this.isAuthenticatedSubject.next(true);
        } else {
          // If refresh fails, logout user
          this.logout('Token refresh failed');
          throw new Error('Token refresh failed');
        }
        return response;
      }),
      catchError((error: any) => {
        // Handle different types of refresh token failures
        let logoutReason = 'Token refresh error';
        
        if (error.status === 401) {
          logoutReason = 'Refresh token expired or invalid';
        } else if (error.status === 403) {
          logoutReason = 'Refresh token forbidden';
        } else if (error.status === 400) {
          logoutReason = 'Invalid refresh token format';
        }
        
        this.logout(logoutReason);
        return this.handleError(error);
      })
    );
  }

  /**
   * Logout user and clear all tokens
   */
  logout(reason?: string): void {
    // Log the reason for logout for debugging
    if (reason) {
      console.log('Logout reason:', reason);
      
      // Show appropriate notification based on reason
      if (reason.includes('expired') || reason.includes('invalid')) {
        this.notificationService.showSessionExpired();
      } else if (reason.includes('refresh')) {
        this.notificationService.showTokenRefreshFailed();
      }
    }

    // Call logout endpoint to invalidate session on server
    const accessToken = this.getAccessToken();
    if (accessToken) {
      this.http.post(`${this.AUTH_API_URL}/logout`, {}).subscribe({
        error: (error) => console.error('Logout error:', error)
      });
    }

    this.clearAuthTokens();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    
    // Only navigate if not already on login page to avoid navigation loops
    if (this.router.url !== '/login') {
      this.router.navigate(['/login']);
    }
  }

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    return Cookies.get('access_token') || null;
  }

  /**
   * Get current refresh token
   */
  getRefreshToken(): string | null {
    return Cookies.get('refresh_token') || null;
  }

  /**
   * Check if user is currently authenticated
   */
  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }

    try {
      // Check if token is expired (basic JWT expiration check)
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Math.floor(Date.now() / 1000);
      return payload.exp > currentTime;
    } catch (error) {
      return false;
    }
  }

  /**
   * Get current user information
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check authentication status on service initialization
   */
  private checkAuthenticationStatus(): void {
    if (this.isAuthenticated()) {
      // If we have a valid token, try to get user info
      this.getUserProfile().subscribe({
        next: (user) => {
          this.currentUserSubject.next(user);
          this.isAuthenticatedSubject.next(true);
        },
        error: () => {
          // If getting user profile fails, try to refresh token
          this.refreshToken().subscribe({
            error: () => {
              this.logout();
            }
          });
        }
      });
    }
  }

  /**
   * Get user profile from server
   */
  private getUserProfile(): Observable<User> {
    return this.http.get<User>(`${this.USER_API_URL}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Store authentication tokens in secure cookies
   */
  private setAuthTokens(accessToken: string, refreshToken: string): void {
    // Set cookies with secure options
    const cookieOptions = {
      secure: true, // Only send over HTTPS in production
      sameSite: 'strict' as const,
      expires: 7 // 7 days for refresh token
    };

    // Access token expires in 1 hour
    Cookies.set('access_token', accessToken, {
      ...cookieOptions,
      expires: 1/24 // 1 hour
    });

    // Refresh token expires in 7 days
    Cookies.set('refresh_token', refreshToken, cookieOptions);
  }

  /**
   * Clear all authentication tokens
   */
  private clearAuthTokens(): void {
    Cookies.remove('access_token');
    Cookies.remove('refresh_token');
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Auth Service Error:', error);
    
    let errorMessage = 'An unexpected error occurred';
    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    return throwError(() => new Error(errorMessage));
  }
}