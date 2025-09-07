import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTabChangeEvent } from '@angular/material/tabs';

import { WorkspaceModuleService, AvailableModule } from '../../../core/services/workspace-module.service';

@Component({
  selector: 'app-module-store-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSelectModule
  ],
  templateUrl: './module-store-dialog.component.html',
  styleUrl: './module-store-dialog.component.scss'
})
export class ModuleStoreDialogComponent implements OnInit {
  private workspaceModuleService = inject(WorkspaceModuleService);
  private snackBar = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<ModuleStoreDialogComponent>);

  // Component state
  availableModules = signal<AvailableModule[]>([]);
  filteredModules = signal<AvailableModule[]>([]);
  isLoading = signal(false);
  isInstalling = signal<string | null>(null);
  selectedCategory = signal<string | null>(null);
  selectedTab = signal(0);

  // Available categories
  readonly categories = [
    'CUSTOMER_MANAGEMENT',
    'ORDER_MANAGEMENT', 
    'PRODUCT_MANAGEMENT',
    'INVENTORY_MANAGEMENT',
    'FINANCIAL_MANAGEMENT',
    'ANALYTICS_REPORTING',
    'COMMUNICATION',
    'PROJECT_MANAGEMENT'
  ];

  async ngOnInit() {
    await this.loadAvailableModules();
  }

  async loadAvailableModules() {
    this.isLoading.set(true);
    try {
      let modules: AvailableModule[] = [];
      
      if (this.selectedTab() === 0) {
        // Featured modules
        modules = await this.workspaceModuleService.getAvailableModules(undefined, true);
      } else {
        // All modules (optionally filtered by category)
        modules = await this.workspaceModuleService.getAvailableModules(this.selectedCategory() || undefined);
      }
      
      this.availableModules.set(modules);
      this.filteredModules.set(modules);
    } catch (error) {
      this.snackBar.open('Failed to load available modules', 'Close', { duration: 3000 });
    } finally {
      this.isLoading.set(false);
    }
  }

  async onTabChange(event: MatTabChangeEvent) {
    this.selectedTab.set(event.index);
    await this.loadAvailableModules();
  }

  async onCategoryChange(category: string | null) {
    this.selectedCategory.set(category);
    if (this.selectedTab() === 1) {
      await this.loadAvailableModules();
    }
  }

  async installModule(module: AvailableModule) {
    if (this.isModuleInstalled(module.module_code)) {
      this.snackBar.open('Module is already installed', 'Close', { duration: 3000 });
      return;
    }

    this.isInstalling.set(module.module_code);
    
    try {
      const result = await this.workspaceModuleService.installModule(module.module_code);
      this.snackBar.open(`${module.name} installed successfully!`, 'Close', { duration: 3000 });
      
      // Close dialog after successful installation
      this.dialogRef.close(result);
    } catch (error: any) {
      this.snackBar.open(error.message || 'Failed to install module', 'Close', { duration: 5000 });
    } finally {
      this.isInstalling.set(null);
    }
  }

  isModuleInstalled(moduleCode: string): boolean {
    return this.workspaceModuleService.isModuleInstalled(moduleCode);
  }

  getCategoryDisplayName(category: string): string {
    const categoryMap: Record<string, string> = {
      'CUSTOMER_MANAGEMENT': 'Customer Management',
      'ORDER_MANAGEMENT': 'Order Management',
      'PRODUCT_MANAGEMENT': 'Product Management',
      'INVENTORY_MANAGEMENT': 'Inventory Management',
      'FINANCIAL_MANAGEMENT': 'Financial Management',
      'ANALYTICS_REPORTING': 'Analytics & Reporting',
      'COMMUNICATION': 'Communication',
      'PROJECT_MANAGEMENT': 'Project Management',
      'HR_MANAGEMENT': 'HR Management',
      'MARKETING': 'Marketing',
      'INTEGRATIONS': 'Integrations',
      'ADMINISTRATION': 'Administration'
    };
    return categoryMap[category] || category;
  }

  getCategoryIcon(category: string): string {
    const iconMap: Record<string, string> = {
      'CUSTOMER_MANAGEMENT': 'people',
      'ORDER_MANAGEMENT': 'shopping_cart',
      'PRODUCT_MANAGEMENT': 'inventory_2',
      'INVENTORY_MANAGEMENT': 'warehouse',
      'FINANCIAL_MANAGEMENT': 'account_balance',
      'ANALYTICS_REPORTING': 'analytics',
      'COMMUNICATION': 'chat',
      'PROJECT_MANAGEMENT': 'assignment',
      'HR_MANAGEMENT': 'badge',
      'MARKETING': 'campaign',
      'INTEGRATIONS': 'hub',
      'ADMINISTRATION': 'admin_panel_settings'
    };
    return iconMap[category] || 'extension';
  }

  getComplexityColor(complexity: string): string {
    switch (complexity.toLowerCase()) {
      case 'low': return 'primary';
      case 'medium': return 'accent';
      case 'high': return 'warn';
      default: return 'primary';
    }
  }

  getComplexityIcon(complexity: string): string {
    switch (complexity.toLowerCase()) {
      case 'low': return 'speed';
      case 'medium': return 'trending_up';
      case 'high': return 'rocket_launch';
      default: return 'help';
    }
  }

  close() {
    this.dialogRef.close();
  }
}