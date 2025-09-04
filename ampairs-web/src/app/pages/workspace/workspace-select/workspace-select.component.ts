import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDividerModule} from '@angular/material/divider';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatListModule} from '@angular/material/list';
import {MatChipsModule} from '@angular/material/chips';
import {MatRippleModule} from '@angular/material/core';
import {MatMenuModule} from '@angular/material/menu';
import {WorkspaceListItem, WorkspaceService} from '../../../core/services/workspace.service';
import {AuthService, User} from '../../../core/services/auth.service';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-workspace-select',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDividerModule,
    MatToolbarModule,
    MatListModule,
    MatChipsModule,
    MatRippleModule,
    MatMenuModule
  ],
  templateUrl: './workspace-select.component.html',
  styleUrl: './workspace-select.component.scss'
})
export class WorkspaceSelectComponent implements OnInit {
  workspaces: WorkspaceListItem[] = [];
  isLoading = false;
  isSelecting = false;
  selectedWorkspaceId = '';
  currentUser$: Observable<User | null>;

  constructor(
    private workspaceService: WorkspaceService,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    this.loadWorkspaces();
  }

  async loadWorkspaces(): Promise<void> {
    this.isLoading = true;

    try {
      const workspaces = await this.workspaceService.getUserWorkspaces();
      this.workspaces = workspaces;
      this.isLoading = false;

      // If no workspaces found, show create workspace option
      if (workspaces.length === 0) {
        this.showNoWorkspacesMessage();
      }
    } catch (error: any) {
      this.isLoading = false;
      console.error('Failed to load workspaces:', error);
      this.showError('Failed to load workspaces. Please try again.');
    }
  }

  selectWorkspace(workspace: WorkspaceListItem): void {
    if (this.isSelecting) return;

    this.isSelecting = true;
    this.selectedWorkspaceId = workspace.id;

    // Get full workspace details
    this.workspaceService.getWorkspaceById(workspace.id).subscribe({
      next: (fullWorkspace) => {
        // Set as current workspace
        this.workspaceService.setCurrentWorkspace(fullWorkspace);

        this.snackBar.open(`Switched to ${workspace.name}`, 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });

        // Navigate to workspace using slug
        this.router.navigate(['/w', fullWorkspace.slug]);
      },
      error: (error) => {
        this.isSelecting = false;
        this.selectedWorkspaceId = '';
        console.error('Failed to select workspace:', error);
        this.showError('Failed to select workspace. Please try again.');
      }
    });
  }

  createNewWorkspace(): void {
    this.router.navigate(['/workspace/create']);
  }

  logout(): void {
    this.authService.logout('User logged out from workspace selection');
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  formatLastActivity(lastActivity?: string): string {
    if (!lastActivity) return 'No recent activity';

    const now = new Date();
    const activityDate = new Date(lastActivity);
    const diffMs = now.getTime() - activityDate.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffMs / (1000 * 60));

    if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  getWorkspaceTypeIcon(workspaceType: string): string {
    switch (workspaceType.toLowerCase()) {
      case 'business':
        return 'business';
      case 'personal':
        return 'person';
      case 'team':
        return 'group';
      case 'enterprise':
        return 'corporate_fare';
      default:
        return 'workspace_premium';
    }
  }

  getSubscriptionColor(plan: string): string {
    switch (plan.toLowerCase()) {
      case 'free':
        return 'accent';
      case 'basic':
        return 'primary';
      case 'premium':
        return 'warn';
      case 'enterprise':
        return '';
      default:
        return 'accent';
    }
  }

  private showNoWorkspacesMessage(): void {
    this.snackBar.open('No workspaces found. Create your first workspace to get started!', 'Create Workspace', {
      duration: 8000,
      panelClass: ['info-snackbar']
    }).onAction().subscribe(() => {
      this.createNewWorkspace();
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
