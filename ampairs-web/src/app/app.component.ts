import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoadingSpinnerComponent } from './shared/components/loading-spinner/loading-spinner.component';
import { ThemeService } from './core/services/theme.service';

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
export class AppComponent implements OnInit {
  title = 'ampairs-web';
  
  // Inject ThemeService to ensure it initializes when the app starts
  private themeService = inject(ThemeService);

  ngOnInit(): void {
    // The ThemeService constructor and initializeTheme() will be called automatically
    // when the service is injected, ensuring themes are applied before any component renders
  }
}