import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {Subject, takeUntil} from 'rxjs';

// Angular Material Imports
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTabsModule} from '@angular/material/tabs';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatChipsModule} from '@angular/material/chips';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatBadgeModule} from '@angular/material/badge';
import {MatMenuModule} from '@angular/material/menu';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatDividerModule} from '@angular/material/divider';

// Services and Models
import {WorkspaceModuleService} from '../../../core/services/workspace-module.service';
import {
  MasterModule,
  ModuleCategory,
  ModuleDashboardResponse,
  ModuleSearchRequest,
  WorkspaceModule,
  WorkspaceModuleStatus
} from '../../../core/models/workspace-module.interface';

@Component({
  selector: 'app-workspace-modules',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressBarModule,
    MatBadgeModule,
    MatMenuModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatDividerModule
  ],
  templateUrl: './workspace-modules.component.html',
  styleUrls: ['./workspace-modules.component.scss']
})
export class WorkspaceModulesComponent implements OnInit, OnDestroy {
  // Data properties
  installedModules: WorkspaceModule[] = [];
  availableModules: MasterModule[] = [];
  dashboardData: ModuleDashboardResponse | null = null;
  // UI state
  loading = false;
  searchQuery = '';
  selectedCategory = '';
  selectedStatus = '';
  currentPage = 0;
  pageSize = 12;
  totalElements = 0;
  // Filter options
  categories = Object.values(ModuleCategory);
  statuses = Object.values(WorkspaceModuleStatus);
  // Selected tab
  selectedTabIndex = 0;
  private destroy$ = new Subject<void>();

  constructor(
    private moduleService: WorkspaceModuleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadInstalledModules();
    this.subscribeToUpdates();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load dashboard analytics
   */
  loadDashboardData(): void {
    this.loading = true;
    this.moduleService.getModuleDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dashboard) => {
          this.dashboardData = dashboard;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading dashboard:', error);
          this.showError('Failed to load dashboard data');
          this.loading = false;
        }
      });
  }

  /**
   * Load installed modules with current filters
   */
  loadInstalledModules(): void {
    const searchRequest: ModuleSearchRequest = {
      query: this.searchQuery || undefined,
      category: this.selectedCategory || undefined,
      status: this.selectedStatus as WorkspaceModuleStatus || undefined,
      page: this.currentPage,
      size: this.pageSize,
      sort_by: 'display_order',
      sort_direction: 'asc'
    };

    this.moduleService.searchInstalledModules(searchRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.installedModules = response.modules;
          this.totalElements = response.total_elements;
        },
        error: (error) => {
          console.error('Error loading modules:', error);
          this.showError('Failed to load installed modules');
        }
      });
  }

  /**
   * Load available modules for installation
   */
  loadAvailableModules(): void {
    this.moduleService.getAvailableModules()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.availableModules = [
            ...response.essential_modules,
            ...response.recommended_modules,
            ...response.available_modules
          ];
        },
        error: (error) => {
          console.error('Error loading available modules:', error);
          this.showError('Failed to load available modules');
        }
      });
  }

  /**
   * Subscribe to service updates
   */
  subscribeToUpdates(): void {
    this.moduleService.installedModules$
      .pipe(takeUntil(this.destroy$))
      .subscribe(modules => {
        this.installedModules = modules;
      });

    this.moduleService.dashboardData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(dashboard => {
        this.dashboardData = dashboard;
      });
  }

  /**
   * Handle tab change
   */
  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    if (index === 1 && this.availableModules.length === 0) {
      this.loadAvailableModules();
    }
  }

  /**
   * Handle search input change
   */
  onSearchChange(): void {
    this.currentPage = 0;
    this.loadInstalledModules();
  }

  /**
   * Handle filter change
   */
  onFilterChange(): void {
    this.currentPage = 0;
    this.loadInstalledModules();
  }

  /**
   * Handle page change
   */
  onPageChange(event: any): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadInstalledModules();
  }

  /**
   * Toggle module enabled/disabled status
   */
  toggleModuleStatus(module: WorkspaceModule): void {
    this.moduleService.toggleModuleStatus(module.id, !module.enabled)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess(`Module ${module.enabled ? 'disabled' : 'enabled'} successfully`);
          this.loadInstalledModules();
        },
        error: (error) => {
          console.error('Error toggling module status:', error);
          this.showError('Failed to update module status');
        }
      });
  }

  /**
   * Install a module
   */
  installModule(masterModule: MasterModule): void {
    const installRequest = {
      master_module_id: masterModule.id,
      display_order: this.installedModules.length + 1
    };

    this.moduleService.installModule(installRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess(`${masterModule.name} installed successfully`);
          this.loadInstalledModules();
          this.loadDashboardData();
          this.selectedTabIndex = 0; // Switch to installed modules tab
        },
        error: (error) => {
          console.error('Error installing module:', error);
          this.showError(`Failed to install ${masterModule.name}`);
        }
      });
  }

  /**
   * Uninstall a module
   */
  uninstallModule(module: WorkspaceModule): void {
    if (confirm(`Are you sure you want to uninstall ${module.effective_name}? This action cannot be undone.`)) {
      this.moduleService.uninstallModule(module.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.showSuccess(`${module.effective_name} uninstalled successfully`);
            this.loadInstalledModules();
            this.loadDashboardData();
          },
          error: (error) => {
            console.error('Error uninstalling module:', error);
            this.showError(`Failed to uninstall ${module.effective_name}`);
          }
        });
    }
  }

  /**
   * Configure module
   */
  configureModule(module: WorkspaceModule): void {
    this.router.navigate(['/workspace/modules', module.id, 'configure']);
  }

  /**
   * View module analytics
   */
  viewModuleAnalytics(module: WorkspaceModule): void {
    this.router.navigate(['/workspace/modules', module.id, 'analytics']);
  }

  /**
   * Update module
   */
  updateModule(module: WorkspaceModule): void {
    this.moduleService.updateModule(module.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess(`${module.effective_name} updated successfully`);
          this.loadInstalledModules();
        },
        error: (error) => {
          console.error('Error updating module:', error);
          this.showError(`Failed to update ${module.effective_name}`);
        }
      });
  }

  /**
   * Reset module configuration
   */
  resetModuleConfiguration(module: WorkspaceModule): void {
    if (confirm(`Are you sure you want to reset ${module.effective_name} to default configuration?`)) {
      this.moduleService.resetModuleConfiguration(module.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.showSuccess(`${module.effective_name} reset to default configuration`);
            this.loadInstalledModules();
          },
          error: (error) => {
            console.error('Error resetting module:', error);
            this.showError(`Failed to reset ${module.effective_name}`);
          }
        });
    }
  }

  /**
   * Get module status color
   */
  getModuleStatusColor(module: WorkspaceModule): string {
    if (!module.enabled) return 'warn';
    if (!module.is_operational) return 'warn';
    if (module.needs_attention) return 'accent';
    return 'primary';
  }

  /**
   * Get module status icon
   */
  getModuleStatusIcon(module: WorkspaceModule): string {
    if (!module.enabled) return 'toggle_off';
    if (!module.is_operational) return 'warning';
    if (module.needs_attention) return 'priority_high';
    return 'toggle_on';
  }

  /**
   * Get health score color
   */
  getHealthScoreColor(score: number): string {
    if (score >= 0.8) return 'primary';
    if (score >= 0.6) return 'accent';
    return 'warn';
  }

  /**
   * Format storage size
   */
  formatStorageSize(sizeInMb: number): string {
    if (sizeInMb < 1024) return `${sizeInMb} MB`;
    return `${(sizeInMb / 1024).toFixed(1)} GB`;
  }

  /**
   * Check if module is already installed
   */
  isModuleInstalled(masterModule: MasterModule): boolean {
    return this.installedModules.some(module =>
      module.master_module.id === masterModule.id
    );
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = '';
    this.selectedStatus = '';
    this.currentPage = 0;
    this.loadInstalledModules();
  }

  /**
   * Refresh data
   */
  refresh(): void {
    this.loadDashboardData();
    this.loadInstalledModules();
    if (this.selectedTabIndex === 1) {
      this.loadAvailableModules();
    }
  }

  /**
   * Show success message
   */
  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  /**
   * Show error message
   */
  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
