import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, switchMap, filter, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip interceptor for authentication endpoints
    if (this.isAuthEndpoint(req.url)) {
      return next.handle(req);
    }

    // Add authorization header if token exists
    const authReq = this.addTokenHeader(req);

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle 401 Unauthorized errors (token expired)
        if (error.status === 401 && !this.isAuthEndpoint(req.url)) {
          return this.handle401Error(authReq, next);
        }

        // Handle other errors
        return throwError(() => error);
      })
    );
  }

  private addTokenHeader(request: HttpRequest<any>): HttpRequest<any> {
    const token = this.authService.getAccessToken();
    
    if (token) {
      return request.clone({
        headers: request.headers.set('Authorization', `Bearer ${token}`)
      });
    }
    
    return request;
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.authService.getRefreshToken();
      
      if (refreshToken) {
        return this.authService.refreshToken().pipe(
          switchMap((response: any) => {
            this.isRefreshing = false;
            
            if (response.access_token && response.refresh_token) {
              this.refreshTokenSubject.next(response.access_token);
              // Retry the failed request with new token
              return next.handle(this.addTokenHeader(request));
            } else {
              // Refresh failed - could be refresh token expired
              console.log('Token refresh failed, redirecting to login');
              this.refreshTokenSubject.next('REFRESH_FAILED');
              this.notificationService.showSessionExpired();
              // AuthService.logout() already handles navigation
              return throwError(() => new Error('Token refresh failed - redirecting to login'));
            }
          }),
          catchError((error) => {
            this.isRefreshing = false;
            console.log('Token refresh error:', error);
            
            // Check if error is due to refresh token expiration
            if (error.status === 401 || error.status === 403) {
              console.log('Refresh token expired, redirecting to login');
              this.notificationService.showSessionExpired();
            } else {
              this.notificationService.showTokenRefreshFailed();
            }
            
            // Notify waiting requests that refresh failed
            this.refreshTokenSubject.next('REFRESH_FAILED');
            // AuthService.logout() already handles navigation
            return throwError(() => new Error('Authentication failed - redirecting to login'));
          })
        );
      } else {
        // No refresh token available, redirect to login
        console.log('No refresh token available, redirecting to login');
        this.isRefreshing = false;
        this.notificationService.showSessionExpired();
        this.authService.logout();
        this.router.navigate(['/login']);
        return throwError(() => new Error('No refresh token available - redirecting to login'));
      }
    } else {
      // If refresh is in progress, wait for it to complete
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap((token) => {
          if (token === 'REFRESH_FAILED') {
            // Refresh failed, redirect to login
            this.router.navigate(['/login']);
            return throwError(() => new Error('Authentication failed'));
          }
          return next.handle(this.addTokenHeader(request));
        })
      );
    }
  }

  private isAuthEndpoint(url: string): boolean {
    // List of endpoints that should not trigger token refresh
    const authEndpoints = [
      '/auth/v1/init',
      '/auth/v1/verify',
      '/auth/v1/refresh_token',
      '/auth/v1/logout'
    ];
    
    return authEndpoints.some(endpoint => url.includes(endpoint));
  }
}