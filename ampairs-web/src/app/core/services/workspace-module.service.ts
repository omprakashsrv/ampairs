import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, Observable, throwError, of} from 'rxjs';
import {map, tap, catchError} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {WorkspaceService} from './workspace.service';
import {
  AvailableModulesResponse,
  BulkOperationRequest,
  BulkOperationResponse,
  MasterModule,
  ModuleActionResponse,
  ModuleCategory,
  ModuleConfigurationRequest,
  ModuleDashboardResponse,
  ModuleInstallationRequest,
  ModuleSearchRequest,
  ModuleSearchResponse,
  WorkspaceModule,
  WorkspaceModuleStatus
} from '../models/workspace-module.interface';

/**
 * Workspace Module Management Service
 *
 * Provides comprehensive module management functionality including:
 * - Module discovery and installation
 * - Configuration and customization
 * - Analytics and monitoring
 * - Bulk operations
 */
@Injectable({
  providedIn: 'root'
})
export class WorkspaceModuleService {
  private readonly WORKSPACE_API_URL = `${environment.apiBaseUrl}/workspace/v1`;
  private readonly GLOBAL_MODULES_API_URL = `${environment.apiBaseUrl}/workspace/v1/modules`;
  private readonly USE_MOCK_DATA = true; // TODO: Remove when backend is implemented

  // State management
  private installedModulesSubject = new BehaviorSubject<WorkspaceModule[]>([]);
  public installedModules$ = this.installedModulesSubject.asObservable();
  private dashboardDataSubject = new BehaviorSubject<ModuleDashboardResponse | null>(null);
  public dashboardData$ = this.dashboardDataSubject.asObservable();

  constructor(
    private http: HttpClient,
    private workspaceService: WorkspaceService
  ) {
  }

  private getCurrentWorkspaceId(): string | null {
    const workspace = this.workspaceService.getCurrentWorkspace();
    return workspace?.id || null;
  }

  private handleError(error: any): Observable<never> {
    console.error('Workspace Module Service Error:', error);
    let errorMessage = 'An unexpected error occurred';
    
    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    return throwError(() => new Error(errorMessage));
  }

  private getMockDashboardData(): ModuleDashboardResponse {
    return {
      total_modules: 8,
      active_modules: 6,
      inactive_modules: 2,
      modules_needing_attention: 1,
      modules_needing_updates: 3,
      storage_usage_mb: 245,
      most_used_modules: [],
      least_used_modules: [],
      category_distribution: {
        'CUSTOMER_MANAGEMENT': 2,
        'SALES_MANAGEMENT': 1,
        'FINANCIAL_MANAGEMENT': 2,
        'ANALYTICS_REPORTING': 1,
        'INVENTORY_MANAGEMENT': 2
      },
      usage_trends: {},
      health_overview: {
        overall_health_score: 0.85,
        healthy_modules: 6,
        warning_modules: 1,
        critical_modules: 1,
        error_rate: 0.02,
        user_satisfaction: 0.92
      }
    };
  }

  private getMockModuleSearchResponse(): ModuleSearchResponse {
    return {
      modules: [],
      total_elements: 0,
      total_pages: 0,
      current_page: 0,
      page_size: 12,
      has_next: false,
      has_previous: false,
      search_metadata: {
        applied_filters: {},
        available_categories: Object.values(ModuleCategory),
        available_statuses: Object.values(WorkspaceModuleStatus),
        featured_count: 0,
        installed_count: 0,
        enabled_count: 0
      }
    };
  }

  /**
   * Get workspace module overview and basic information
   */
  getModuleOverview(): Observable<{ [key: string]: any }> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.get<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Get detailed information about a specific module
   */
  getModule(moduleId: string): Observable<{ [key: string]: any }> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.get<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/${moduleId}`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Perform an action on a specific module
   */
  performModuleAction(moduleId: string, action: string, parameters?: {
    [key: string]: any
  }): Observable<ModuleActionResponse> {
    const params = new HttpParams().set('action', action);
    const body = parameters ? {parameters} : {};

    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.post<ModuleActionResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/${moduleId}/action`, body, {params})
      .pipe(
        tap(() => this.refreshInstalledModules()),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Search and list installed modules with filtering and pagination
   */
  searchInstalledModules(searchRequest: ModuleSearchRequest = {}): Observable<ModuleSearchResponse> {
    if (this.USE_MOCK_DATA) {
      const mockData = this.getMockModuleSearchResponse();
      this.installedModulesSubject.next(mockData.modules);
      return of(mockData);
    }

    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    let params = new HttpParams();

    if (searchRequest.query) params = params.set('query', searchRequest.query);
    if (searchRequest.category) params = params.set('category', searchRequest.category);
    if (searchRequest.status) params = params.set('status', searchRequest.status);
    if (searchRequest.enabled !== undefined) params = params.set('enabled', searchRequest.enabled.toString());
    if (searchRequest.featured !== undefined) params = params.set('featured', searchRequest.featured.toString());
    if (searchRequest.sort_by) params = params.set('sort_by', searchRequest.sort_by);
    if (searchRequest.sort_direction) params = params.set('sort_direction', searchRequest.sort_direction);
    if (searchRequest.page !== undefined) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size !== undefined) params = params.set('size', searchRequest.size.toString());

    return this.http.get<ModuleSearchResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/search`, {params})
      .pipe(
        tap(result => this.installedModulesSubject.next(result.modules)),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Get available modules that can be installed
   */
  getAvailableModules(businessType?: string): Observable<AvailableModulesResponse> {
    let params = new HttpParams();
    if (businessType) params = params.set('business_type', businessType);

    return this.http.get<AvailableModulesResponse>(`${this.GLOBAL_MODULES_API_URL}/master-modules/available`, {params})
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Get master module details
   */
  getMasterModule(moduleId: string): Observable<MasterModule> {
    return this.http.get<MasterModule>(`${this.GLOBAL_MODULES_API_URL}/master-modules/${moduleId}`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Search master modules (available for installation)
   */
  searchMasterModules(searchRequest: ModuleSearchRequest = {}): Observable<{ modules: MasterModule[], total: number }> {
    let params = new HttpParams();

    if (searchRequest.query) params = params.set('query', searchRequest.query);
    if (searchRequest.category) params = params.set('category', searchRequest.category);
    if (searchRequest.featured !== undefined) params = params.set('featured', searchRequest.featured.toString());
    if (searchRequest.sort_by) params = params.set('sort_by', searchRequest.sort_by);
    if (searchRequest.sort_direction) params = params.set('sort_direction', searchRequest.sort_direction);
    if (searchRequest.page !== undefined) params = params.set('page', searchRequest.page.toString());
    if (searchRequest.size !== undefined) params = params.set('size', searchRequest.size.toString());

    return this.http.get<any>(`${this.GLOBAL_MODULES_API_URL}/master-modules/search`, {params})
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Install a new module
   */
  installModule(installRequest: ModuleInstallationRequest): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.post<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/install`, installRequest)
      .pipe(
        tap(() => this.refreshInstalledModules()),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Uninstall a module
   */
  uninstallModule(moduleId: string, preserveData: boolean = false): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    const params = new HttpParams().set('preserve_data', preserveData.toString());

    return this.http.delete<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/${moduleId}`, {params})
      .pipe(
        tap(() => this.refreshInstalledModules()),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Update module configuration
   */
  updateModuleConfiguration(moduleId: string, configRequest: ModuleConfigurationRequest): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.put<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/${moduleId}/configuration`, configRequest)
      .pipe(
        tap(() => this.refreshInstalledModules()),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Enable/disable module
   */
  toggleModuleStatus(moduleId: string, enabled: boolean): Observable<any> {
    const action = enabled ? 'enable' : 'disable';
    return this.performModuleAction(moduleId, action);
  }

  /**
   * Bulk operations on multiple modules
   */
  performBulkOperation(bulkRequest: BulkOperationRequest): Observable<BulkOperationResponse> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.post<BulkOperationResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/bulk`, bulkRequest)
      .pipe(
        tap(() => this.refreshInstalledModules()),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Get module dashboard and analytics
   */
  getModuleDashboard(): Observable<ModuleDashboardResponse> {
    if (this.USE_MOCK_DATA) {
      const mockData = this.getMockDashboardData();
      this.dashboardDataSubject.next(mockData);
      return of(mockData);
    }

    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.get<ModuleDashboardResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/dashboard`)
      .pipe(
        tap(dashboard => this.dashboardDataSubject.next(dashboard)),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Get module analytics for a specific module
   */
  getModuleAnalytics(moduleId: string, period: string = '30d'): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    const params = new HttpParams().set('period', period);

    return this.http.get<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/${moduleId}/analytics`, {params})
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Export module configuration
   */
  exportModuleConfiguration(moduleIds?: string[]): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    const body = moduleIds ? {module_ids: moduleIds} : {};

    return this.http.post<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/export`, body)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Import module configuration
   */
  importModuleConfiguration(configData: any): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.post<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/import`, configData)
      .pipe(
        tap(() => this.refreshInstalledModules()),
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Get module categories
   */
  getModuleCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.GLOBAL_MODULES_API_URL}/categories`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Check for module updates
   */
  checkForUpdates(): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.get<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/updates`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Update a specific module to latest version
   */
  updateModule(moduleId: string): Observable<any> {
    return this.performModuleAction(moduleId, 'update');
  }

  /**
   * Reset module to default configuration
   */
  resetModuleConfiguration(moduleId: string): Observable<any> {
    return this.performModuleAction(moduleId, 'reset');
  }

  /**
   * Get module health status
   */
  getModuleHealthStatus(moduleId: string): Observable<any> {
    const workspaceId = this.getCurrentWorkspaceId();
    if (!workspaceId) {
      return throwError(() => new Error('No workspace selected'));
    }

    return this.http.get<any>(`${this.WORKSPACE_API_URL}/${workspaceId}/modules/${moduleId}/health`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Run module diagnostics
   */
  runModuleDiagnostics(moduleId: string): Observable<any> {
    return this.performModuleAction(moduleId, 'diagnose');
  }

  /**
   * Get current installed modules (synchronous access to cached data)
   */
  getCurrentInstalledModules(): WorkspaceModule[] {
    return this.installedModulesSubject.value;
  }

  /**
   * Get current dashboard data (synchronous access to cached data)
   */
  getCurrentDashboardData(): ModuleDashboardResponse | null {
    return this.dashboardDataSubject.value;
  }

  /**
   * Clear cached data
   */
  clearCache(): void {
    this.installedModulesSubject.next([]);
    this.dashboardDataSubject.next(null);
  }

  /**
   * Private helper method to refresh installed modules
   */
  private refreshInstalledModules(): void {
    this.searchInstalledModules({size: 100}).subscribe();
  }
}
