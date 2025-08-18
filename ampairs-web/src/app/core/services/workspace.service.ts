import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description?: string;
  workspace_type: string;
  avatar_url?: string;
  is_active: boolean;
  subscription_plan: string;
  max_members: number;
  storage_limit_gb: number;
  storage_used_gb: number;
  timezone: string;
  language: string;
  created_by: string;
  created_at: string;
  updated_at: string;
  last_activity_at?: string;
  trial_expires_at?: string;
  member_count?: number;
  is_trial?: boolean;
  storage_percentage?: number;
}

export interface WorkspaceListItem {
  id: string;
  name: string;
  slug: string;
  description?: string;
  workspace_type: string;
  avatar_url?: string;
  subscription_plan: string;
  member_count: number;
  last_activity_at?: string;
  created_at: string;
}

export interface CreateWorkspaceRequest {
  name: string;
  description?: string;
  workspace_type: string;
  avatar_url?: string;
  timezone?: string;
  language?: string;
  slug?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  timestamp: string;
  path?: string;
  trace_id?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    page_number: number;
    page_size: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  total_elements: number;
  total_pages: number;
  first: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  number_of_elements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceService {
  private readonly WORKSPACE_API_URL = `${environment.apiBaseUrl}/workspace/v1`;

  private currentWorkspaceSubject = new BehaviorSubject<Workspace | null>(null);
  public currentWorkspace$ = this.currentWorkspaceSubject.asObservable();
  private workspacesSubject = new BehaviorSubject<WorkspaceListItem[]>([]);
  public workspaces$ = this.workspacesSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadSelectedWorkspace();
  }

  /**
   * Get user's workspaces with pagination
   */
  getUserWorkspaces(page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc'): Observable<WorkspaceListItem[]> {
    const params = {
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortDir
    };

    return this.http.get<any>(`${this.WORKSPACE_API_URL}`, {params})
      .pipe(
        map(response => {
          console.log('Full API Response:', response);
          console.log('Response properties:', Object.keys(response || {}));

          // Check if response has the ApiResponse wrapper structure
          if (response && response.data && response.data.content) {
            console.log('Found ApiResponse wrapper with data.content');
            const workspaces = response.data.content;
            this.workspacesSubject.next(workspaces);
            return workspaces;
          }

          // Check if response is directly the paginated structure
          if (response && response.content) {
            console.log('Found direct paginated response with content');
            const workspaces = response.content;
            this.workspacesSubject.next(workspaces);
            return workspaces;
          }

          // Check if response is directly an array
          if (Array.isArray(response)) {
            console.log('Response is directly an array');
            const workspaces = response;
            this.workspacesSubject.next(workspaces);
            return workspaces;
          }

          console.error('Unexpected response structure. Available properties:', Object.keys(response || {}));
          return [];
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Create a new workspace
   */
  createWorkspace(workspaceData: CreateWorkspaceRequest): Observable<Workspace> {
    return this.http.post<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}`, workspaceData)
      .pipe(
        map(response => {
          const workspace = response.data;
          // Refresh workspace list after creation
          this.getUserWorkspaces().subscribe();
          return workspace;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get workspace by ID
   */
  getWorkspaceById(workspaceId: string): Observable<Workspace> {
    return this.http.get<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}/${workspaceId}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Get workspace by slug
   */
  getWorkspaceBySlug(slug: string): Observable<Workspace> {
    return this.http.get<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}/slug/${slug}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Update workspace
   */
  updateWorkspace(workspaceId: string, workspaceData: Partial<CreateWorkspaceRequest>): Observable<Workspace> {
    return this.http.put<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}/${workspaceId}`, workspaceData)
      .pipe(
        map(response => {
          const workspace = response.data;
          // Update current workspace if it's the one being updated
          if (this.currentWorkspaceSubject.value?.id === workspaceId) {
            this.currentWorkspaceSubject.next(workspace);
          }
          return workspace;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Search workspaces
   */
  searchWorkspaces(query: string, workspaceType?: string, subscriptionPlan?: string): Observable<WorkspaceListItem[]> {
    const params: any = {query};
    if (workspaceType) params.workspaceType = workspaceType;
    if (subscriptionPlan) params.subscriptionPlan = subscriptionPlan;

    return this.http.get<ApiResponse<PaginatedResponse<WorkspaceListItem>>>(`${this.WORKSPACE_API_URL}/search`, {params})
      .pipe(
        map(response => response.data.content),
        catchError(this.handleError)
      );
  }

  /**
   * Check if workspace slug is available
   */
  checkSlugAvailability(slug: string): Observable<boolean> {
    return this.http.get<ApiResponse<{ available: boolean }>>(`${this.WORKSPACE_API_URL}/check-slug/${slug}`)
      .pipe(
        map(response => response.data.available),
        catchError(this.handleError)
      );
  }

  /**
   * Set current workspace and store in localStorage
   */
  setCurrentWorkspace(workspace: Workspace): void {
    this.currentWorkspaceSubject.next(workspace);
    localStorage.setItem('selected_workspace', JSON.stringify(workspace));
    // Set workspace header for API requests
    localStorage.setItem('workspace_id', workspace.id);
  }

  /**
   * Get current workspace
   */
  getCurrentWorkspace(): Workspace | null {
    return this.currentWorkspaceSubject.value;
  }

  /**
   * Clear current workspace
   */
  clearCurrentWorkspace(): void {
    this.currentWorkspaceSubject.next(null);
    localStorage.removeItem('selected_workspace');
    localStorage.removeItem('workspace_id');
  }

  /**
   * Check if user has selected a workspace
   */
  hasSelectedWorkspace(): boolean {
    return this.getCurrentWorkspace() !== null;
  }

  /**
   * Load selected workspace from localStorage
   */
  private loadSelectedWorkspace(): void {
    const savedWorkspace = localStorage.getItem('selected_workspace');
    if (savedWorkspace) {
      try {
        const workspace = JSON.parse(savedWorkspace);
        this.currentWorkspaceSubject.next(workspace);
      } catch (error) {
        console.error('Failed to parse saved workspace:', error);
        localStorage.removeItem('selected_workspace');
        localStorage.removeItem('workspace_id');
      }
    }
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Workspace Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    // Extract error message from API response structure
    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    throw new Error(errorMessage);
  }
}
