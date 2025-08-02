import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoadingSpinnerComponent } from './shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, LoadingSpinnerComponent],
  template: `
    <router-outlet></router-outlet>
    <app-loading-spinner></app-loading-spinner>
  `,
  styles: []
})
export class AppComponent {
  title = 'ampairs-web';
}