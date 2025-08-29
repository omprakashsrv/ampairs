import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {
  AvailableModulesResponse,
  BulkOperationRequest,
  BulkOperationResponse,
  MasterModule,
  ModuleActionResponse,
  ModuleConfigurationRequest,
  ModuleDashboardResponse,
  ModuleInstallationRequest,
  ModuleSearchRequest,
  ModuleSearchResponse,
  WorkspaceModule
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
  private readonly baseUrl = `${environment.apiBaseUrl}/workspace/v1/modules`;
  private readonly masterModulesUrl = `${environment.apiBaseUrl}/workspace/v1/master-modules`;

  // State management
  private installedModulesSubject = new BehaviorSubject<WorkspaceModule[]>([]);
  public installedModules$ = this.installedModulesSubject.asObservable();
  private dashboardDataSubject = new BehaviorSubject<ModuleDashboardResponse | null>(null);
  public dashboardData$ = this.dashboardDataSubject.asObservable();

  constructor(private http: HttpClient) {
  }

  /**
   * Get workspace module overview and basic information
   */
  getModuleOverview(): Observable<{ [key: string]: any }> {
    return this.http.get<{ success: boolean; data: any }>(`${this.baseUrl}`)
      .pipe(map(response => response.data));
  }

  /**
   * Get detailed information about a specific module
   */
  getModule(moduleId: string): Observable<{ [key: string]: any }> {
    return this.http.get<{ success: boolean; data: any }>(`${this.baseUrl}/${moduleId}`)
      .pipe(map(response => response.data));
  }

  /**
   * Perform an action on a specific module
   */
  performModuleAction(moduleId: string, action: string, parameters?: {
    [key: string]: any
  }): Observable<ModuleActionResponse> {
    const params = new HttpParams().set('action', action);
    const body = parameters ? {parameters} : {};

    return this.http.post<{
      success: boolean;
      data: ModuleActionResponse
    }>(`${this.baseUrl}/${moduleId}/action`, body, {params})
      .pipe(
        map(response => response.data),
        tap(() => this.refreshInstalledModules())
      );
  }

  /**
   * Search and list installed modules with filtering and pagination
   */
  searchInstalledModules(searchRequest: ModuleSearchRequest = {}): Observable<ModuleSearchResponse> {
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

    return this.http.get<{ success: boolean; data: ModuleSearchResponse }>(`${this.baseUrl}/search`, {params})
      .pipe(
        map(response => response.data),
        tap(result => this.installedModulesSubject.next(result.modules))
      );
  }

  /**
   * Get available modules that can be installed
   */
  getAvailableModules(businessType?: string): Observable<AvailableModulesResponse> {
    let params = new HttpParams();
    if (businessType) params = params.set('business_type', businessType);

    return this.http.get<{
      success: boolean;
      data: AvailableModulesResponse
    }>(`${this.masterModulesUrl}/available`, {params})
      .pipe(map(response => response.data));
  }

  /**
   * Get master module details
   */
  getMasterModule(moduleId: string): Observable<MasterModule> {
    return this.http.get<{ success: boolean; data: MasterModule }>(`${this.masterModulesUrl}/${moduleId}`)
      .pipe(map(response => response.data));
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

    return this.http.get<{ success: boolean; data: any }>(`${this.masterModulesUrl}/search`, {params})
      .pipe(map(response => response.data));
  }

  /**
   * Install a new module
   */
  installModule(installRequest: ModuleInstallationRequest): Observable<any> {
    return this.http.post<{ success: boolean; data: any }>(`${this.baseUrl}/install`, installRequest)
      .pipe(
        map(response => response.data),
        tap(() => this.refreshInstalledModules())
      );
  }

  /**
   * Uninstall a module
   */
  uninstallModule(moduleId: string, preserveData: boolean = false): Observable<any> {
    const params = new HttpParams().set('preserve_data', preserveData.toString());

    return this.http.delete<{ success: boolean; data: any }>(`${this.baseUrl}/${moduleId}`, {params})
      .pipe(
        map(response => response.data),
        tap(() => this.refreshInstalledModules())
      );
  }

  /**
   * Update module configuration
   */
  updateModuleConfiguration(moduleId: string, configRequest: ModuleConfigurationRequest): Observable<any> {
    return this.http.put<{ success: boolean; data: any }>(`${this.baseUrl}/${moduleId}/configuration`, configRequest)
      .pipe(
        map(response => response.data),
        tap(() => this.refreshInstalledModules())
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
    return this.http.post<{ success: boolean; data: BulkOperationResponse }>(`${this.baseUrl}/bulk`, bulkRequest)
      .pipe(
        map(response => response.data),
        tap(() => this.refreshInstalledModules())
      );
  }

  /**
   * Get module dashboard and analytics
   */
  getModuleDashboard(): Observable<ModuleDashboardResponse> {
    return this.http.get<{ success: boolean; data: ModuleDashboardResponse }>(`${this.baseUrl}/dashboard`)
      .pipe(
        map(response => response.data),
        tap(dashboard => this.dashboardDataSubject.next(dashboard))
      );
  }

  /**
   * Get module analytics for a specific module
   */
  getModuleAnalytics(moduleId: string, period: string = '30d'): Observable<any> {
    const params = new HttpParams().set('period', period);

    return this.http.get<{ success: boolean; data: any }>(`${this.baseUrl}/${moduleId}/analytics`, {params})
      .pipe(map(response => response.data));
  }

  /**
   * Export module configuration
   */
  exportModuleConfiguration(moduleIds?: string[]): Observable<any> {
    const body = moduleIds ? {module_ids: moduleIds} : {};

    return this.http.post<{ success: boolean; data: any }>(`${this.baseUrl}/export`, body)
      .pipe(map(response => response.data));
  }

  /**
   * Import module configuration
   */
  importModuleConfiguration(configData: any): Observable<any> {
    return this.http.post<{ success: boolean; data: any }>(`${this.baseUrl}/import`, configData)
      .pipe(
        map(response => response.data),
        tap(() => this.refreshInstalledModules())
      );
  }

  /**
   * Get module categories
   */
  getModuleCategories(): Observable<string[]> {
    return this.http.get<{ success: boolean; data: string[] }>(`${this.masterModulesUrl}/categories`)
      .pipe(map(response => response.data));
  }

  /**
   * Check for module updates
   */
  checkForUpdates(): Observable<any> {
    return this.http.get<{ success: boolean; data: any }>(`${this.baseUrl}/updates`)
      .pipe(map(response => response.data));
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
    return this.http.get<{ success: boolean; data: any }>(`${this.baseUrl}/${moduleId}/health`)
      .pipe(map(response => response.data));
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
