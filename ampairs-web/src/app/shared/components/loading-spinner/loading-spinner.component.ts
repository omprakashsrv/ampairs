import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoadingService } from '../../../core/services/loading.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    @if (loading$ | async) {
      <div class="loading-overlay">
        <div class="loading-container">
          <mat-spinner diameter="50"></mat-spinner>
          <p class="loading-text">Loading...</p>
        </div>
      </div>
    }
  `,
  styles: [`
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 9999;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      background: white;
      padding: 32px;
      border-radius: 12px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
    }

    .loading-text {
      margin: 0;
      color: #666;
      font-size: 16px;
      font-weight: 500;
    }

    mat-spinner {
      --mdc-circular-progress-active-indicator-color: #3f51b5;
    }
  `]
})
export class LoadingSpinnerComponent {
  loading$: Observable<boolean>;

  constructor(private loadingService: LoadingService) {
    this.loading$ = this.loadingService.loading$;
  }
}