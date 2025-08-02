import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

declare var grecaptcha: any;

@Injectable({
  providedIn: 'root'
})
export class RecaptchaService {
  private isLoadedSubject = new BehaviorSubject<boolean>(false);
  public isLoaded$ = this.isLoadedSubject.asObservable();

  constructor() {
    this.loadRecaptchaScript();
  }

  /**
   * Load Google reCAPTCHA script
   */
  private loadRecaptchaScript(): void {
    if (typeof grecaptcha !== 'undefined') {
      this.isLoadedSubject.next(true);
      return;
    }

    const script = document.createElement('script');
    script.src = `https://www.google.com/recaptcha/api.js?render=${environment.recaptcha.siteKey}`;
    script.async = true;
    script.defer = true;
    script.onload = () => {
      this.isLoadedSubject.next(true);
    };
    script.onerror = () => {
      console.error('Failed to load reCAPTCHA script');
      this.isLoadedSubject.next(false);
    };
    
    document.head.appendChild(script);
  }

  /**
   * Execute reCAPTCHA v3 and get token
   */
  executeRecaptcha(action: string): Promise<string> {
    return new Promise((resolve, reject) => {
      if (typeof grecaptcha === 'undefined') {
        reject('reCAPTCHA not loaded');
        return;
      }

      grecaptcha.ready(() => {
        grecaptcha.execute(environment.recaptcha.siteKey, { action })
          .then((token: string) => {
            resolve(token);
          })
          .catch((error: any) => {
            console.error('reCAPTCHA execution error:', error);
            reject(error);
          });
      });
    });
  }

  /**
   * Execute reCAPTCHA for login action
   */
  getLoginToken(): Promise<string> {
    return this.executeRecaptcha('login');
  }

  /**
   * Execute reCAPTCHA for OTP verification action
   */
  getVerifyOtpToken(): Promise<string> {
    return this.executeRecaptcha('verify_otp');
  }

  /**
   * Execute reCAPTCHA for resend OTP action
   */
  getResendOtpToken(): Promise<string> {
    return this.executeRecaptcha('resend_otp');
  }

  /**
   * Check if reCAPTCHA is loaded and ready
   */
  isReady(): boolean {
    return this.isLoadedSubject.value && typeof grecaptcha !== 'undefined';
  }

  /**
   * Wait for reCAPTCHA to be ready
   */
  waitForReady(): Observable<boolean> {
    return this.isLoaded$;
  }
}