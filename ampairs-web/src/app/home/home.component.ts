import {Component, OnInit} from '@angular/core';
import {Router, RouterOutlet} from '@angular/router';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatGridListModule} from '@angular/material/grid-list';
import {CommonModule} from '@angular/common';
import {AuthService, User} from '../core/services/auth.service';
import {Workspace, WorkspaceService} from '../core/services/workspace.service';
import {ThemeService} from '../core/services/theme.service';
import {ThemeSettingsComponent} from '../shared/components/theme-settings/theme-settings.component';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatCardModule,
    MatDividerModule,
    MatDialogModule,
    MatTooltipModule,
    MatGridListModule
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  currentUser$: Observable<User | null>;
  currentWorkspace$: Observable<Workspace | null>;

  constructor(
    private authService: AuthService,
    private workspaceService: WorkspaceService,
    private themeService: ThemeService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.currentUser$ = this.authService.currentUser$;
    this.currentWorkspace$ = this.workspaceService.currentWorkspace$;
  }

  ngOnInit(): void {
    // Component initialization
  }

  editProfile(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.router.navigate(['/w', currentWorkspace.slug, 'profile'], {queryParams: {edit: 'true'}});
    }
  }

  viewSettings(): void {
    this.openThemeSettings();
  }

  openThemeSettings(): void {
    const dialogRef = this.dialog.open(ThemeSettingsComponent, {
      width: '600px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.applied) {
        console.log('Theme settings applied:', result);
      }
    });
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  viewDevices(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.router.navigate(['/w', currentWorkspace.slug, 'devices']);
    }
  }

  switchWorkspace(): void {
    this.router.navigate(['/workspaces']);
  }

  manageMembers(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.router.navigate(['/w', currentWorkspace.slug, 'members']);
    }
  }

  manageModules(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.router.navigate(['/w', currentWorkspace.slug, 'modules']);
    }
  }

  logout(): void {
    // Clear workspace data when logging out
    this.workspaceService.clearCurrentWorkspace();
    this.authService.logout();
  }
}
