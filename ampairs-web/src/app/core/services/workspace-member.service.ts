import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, throwError, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {
  MemberFilters,
  MemberSortOptions,
  MemberStatus,
  PagedMemberResponse,
  SimpleUserRoleResponse,
  UpdateMemberRequest,
  UserRoleResponse,
  WorkspaceMember,
  WorkspaceMemberRole
} from '../models/workspace-member.interface';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  timestamp: string;
  path?: string;
  trace_id?: string;
}

export interface MemberSearchRequest {
  workspace_id?: string;
  page?: number;
  size?: number;
  sort_by?: string;
  sort_direction?: string;
  role?: WorkspaceMemberRole | 'ALL';
  status?: MemberStatus | 'ALL';
  department?: string;
  search_query?: string;
}

export interface InviteMemberRequest {
  email: string;
  role: WorkspaceMemberRole;
  department?: string;
  custom_message?: string;
  notify_member?: boolean;
}

export interface BulkUpdateMemberRequest {
  member_ids: string[];
  role?: WorkspaceMemberRole;
  department?: string;
  status?: MemberStatus;
  reason?: string;
  notify_members?: boolean;
}

export interface MemberActivityLog {
  id: string;
  member_id: string;
  action: string;
  description: string;
  performed_by: string;
  performed_at: string;
  details?: { [key: string]: any };
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceMemberService {
  private readonly MEMBER_API_URL = `${environment.apiBaseUrl}/workspace/v1/members`;
  private readonly WORKSPACE_API_URL = `${environment.apiBaseUrl}/workspace/v1`;

  constructor(private http: HttpClient) {
  }

  /**
   * Get paginated list of workspace members
   */
  getMembers(workspaceId: string, request: MemberSearchRequest = {}): Observable<PagedMemberResponse> {
    let params = new HttpParams();

    if (request.page !== undefined) params = params.set('page', request.page.toString());
    if (request.size !== undefined) params = params.set('size', request.size.toString());
    if (request.sort_by) params = params.set('sortBy', request.sort_by);
    if (request.sort_direction) params = params.set('sortDir', request.sort_direction);
    if (request.role && request.role !== 'ALL') params = params.set('role', request.role);
    if (request.status && request.status !== 'ALL') params = params.set('status', request.status);
    if (request.department) params = params.set('department', request.department);
    if (request.search_query) params = params.set('search_query', request.search_query);

    return this.http.get<{
      content: any[]; 
      page_number: number; 
      page_size: number; 
      total_elements: number; 
      total_pages: number; 
      first: boolean; 
      last: boolean;
    }>(`${this.WORKSPACE_API_URL}/${workspaceId}/members`, {params})
      .pipe(
        map(response => {
          console.log('getMembers - Interceptor-unwrapped response:', response);
          
          // Handle case where response is null/undefined
          if (!response) {
            console.warn('getMembers - Response is null/undefined');
            return {
              content: [],
              page_number: 0,
              page_size: 20,
              total_elements: 0,
              total_pages: 0,
              first: true,
              last: true
            } as PagedMemberResponse;
          }
          
          // Response is already unwrapped by ApiResponseInterceptor
          // Map backend response format to frontend expected format
          const result = {
            content: response.content || [],
            page_number: response.page_number || 0,
            page_size: response.page_size || 20,
            total_elements: response.total_elements || 0,
            total_pages: response.total_pages || 0,
            first: response.first || false,
            last: response.last || false
          } as PagedMemberResponse;
          
          console.log('getMembers - Mapped result:', result);
          return result;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get member details by ID
   */
  getMemberById(workspaceId: string, memberId: string): Observable<WorkspaceMember> {
    return this.http.get<WorkspaceMember>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Get current user's role and permissions in workspace
   */
  getCurrentUserRole(workspaceId: string): Observable<SimpleUserRoleResponse> {
    console.log('Service calling API with workspaceId:', workspaceId);
    const url = `${this.WORKSPACE_API_URL}/${workspaceId}/my-role`;
    console.log('Full URL:', url);

    return this.http.get<{
      user_id: string;
      workspace_id: string;
      current_role: string;
      membership_status: string;
      joined_at: string;
      last_activity?: string;
      role_hierarchy: { [key: string]: boolean };
      permissions: { [key: string]: { [key: string]: boolean } };
      module_access: string[];
    }>(url)
      .pipe(
        map(response => {
          console.log('Service received interceptor-unwrapped response:', response);

          // Handle case where response is null/undefined
          if (!response) {
            console.error('Response is null/undefined');
            throw new Error('Invalid response');
          }

          // Map the UserRoleResponse structure (with snake_case from backend) to SimpleUserRoleResponse
          let data: SimpleUserRoleResponse;
          
          if (response.role_hierarchy && response.permissions) {
            // Map from UserRoleResponse structure (snake_case)
            data = {
              is_owner: response.role_hierarchy['OWNER'] || false,
              is_admin: response.role_hierarchy['ADMIN'] || false,
              can_view_members: response.permissions['members']?.['view'] || false,
              can_invite_members: response.permissions['members']?.['invite'] || false,
              can_manage_workspace: response.permissions['workspace']?.['manage'] || false
            };
            console.log('Mapped UserRoleResponse to SimpleUserRoleResponse:', data);
          }
          // Handle camelCase version as fallback (though unlikely with interceptor)
          else if ((response as any).roleHierarchy && (response as any).permissions) {
            // Map from UserRoleResponse structure (camelCase)
            data = {
              is_owner: (response as any).roleHierarchy?.OWNER || false,
              is_admin: (response as any).roleHierarchy?.ADMIN || false,
              can_view_members: (response as any).permissions?.members?.view || false,
              can_invite_members: (response as any).permissions?.members?.invite || false,
              can_manage_workspace: (response as any).permissions?.workspace?.manage || false
            };
            console.log('Mapped camelCase UserRoleResponse to SimpleUserRoleResponse:', data);
          }
          // Check if it's already in simple format
          else if ((response as any).is_owner !== undefined || (response as any).can_view_members !== undefined) {
            data = response as any;
            console.log('Already in simple format:', data);
          } else {
            console.error('Unknown response structure:', response);
            throw new Error('Unknown response structure');
          }

          return data;
        }),
        catchError(error => {
          console.error('Service catchError triggered:', error);
          return this.handleError(error);
        })
      );
  }

  /**
   * Update member role and permissions
   */
  updateMember(workspaceId: string, memberId: string, updateRequest: UpdateMemberRequest): Observable<WorkspaceMember> {
    return this.http.put<WorkspaceMember>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}`, updateRequest)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Bulk update multiple members
   */
  bulkUpdateMembers(bulkRequest: BulkUpdateMemberRequest): Observable<{
    updated_count: number;
    failed_updates: Array<{ member_id: string; error: string }>
  }> {
    const currentWorkspace = JSON.parse(localStorage.getItem('selected_workspace') || '{}');
    const workspaceId = currentWorkspace?.id;

    if (!workspaceId) {
      return throwError(() => new Error('No current workspace selected'));
    }

    return this.http.put<{
      updated_count: number;
      failed_updates: Array<{ member_id: string; error: string }>
    }>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/bulk`, bulkRequest)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Remove member from workspace
   */
  removeMember(workspaceId: string, memberId: string, reason?: string): Observable<{ message: string }> {
    const body = reason ? {reason} : {};
    return this.http.delete<{
      message: string
    }>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}`, {body})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Bulk remove multiple members
   */
  bulkRemoveMembers(memberIds: string[], reason?: string): Observable<{
    removed_count: number;
    failed_removals: Array<{ member_id: string; error: string }>
  }> {
    const currentWorkspace = JSON.parse(localStorage.getItem('selected_workspace') || '{}');
    const workspaceId = currentWorkspace?.id;

    if (!workspaceId) {
      return throwError(() => new Error('No current workspace selected'));
    }

    const body = {
      member_ids: memberIds,
      reason: reason
    };

    return this.http.delete<{
      removed_count: number;
      failed_removals: Array<{ member_id: string; error: string }>
    }>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/bulk`, {body})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Search members with advanced filtering
   */
  searchMembers(filters: MemberFilters, sortOptions?: MemberSortOptions, page = 0, size = 20): Observable<PagedMemberResponse> {
    const currentWorkspace = JSON.parse(localStorage.getItem('selected_workspace') || '{}');
    const workspaceId = currentWorkspace?.id;

    if (!workspaceId) {
      return throwError(() => new Error('No current workspace selected'));
    }

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters.role && filters.role !== 'ALL') params = params.set('role', filters.role);
    if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters.department) params = params.set('department', filters.department);
    if (filters.search_query) params = params.set('search_query', filters.search_query);

    if (sortOptions) {
      params = params.set('sortBy', sortOptions.sort_by);
      params = params.set('sortDir', sortOptions.sort_direction);
    }

    return this.http.get<{
      content: any[]; 
      page_number: number; 
      page_size: number; 
      total_elements: number; 
      total_pages: number; 
      first: boolean; 
      last: boolean;
    }>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/search`, {params})
      .pipe(
        map(response => {
          console.log('searchMembers - Interceptor-unwrapped response:', response);
          
          // Handle case where response is null/undefined
          if (!response) {
            console.warn('searchMembers - Response is null/undefined');
            return {
              content: [],
              page_number: 0,
              page_size: 20,
              total_elements: 0,
              total_pages: 0,
              first: true,
              last: true
            } as PagedMemberResponse;
          }
          
          // Response is already unwrapped by ApiResponseInterceptor
          // Map backend response format to frontend expected format
          const result = {
            content: response.content || [],
            page_number: response.page_number || 0,
            page_size: response.page_size || 20,
            total_elements: response.total_elements || 0,
            total_pages: response.total_pages || 0,
            first: response.first || false,
            last: response.last || false
          } as PagedMemberResponse;
          
          console.log('searchMembers - Mapped result:', result);
          return result;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get member activity log
   */
  getMemberActivityLog(memberId: string, page = 0, size = 20): Observable<{
    content: MemberActivityLog[];
    total_elements: number
  }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<{
      content: MemberActivityLog[];
      total_elements: number
    }>(`${this.MEMBER_API_URL}/${memberId}/activity`, {params})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Get workspace member statistics
   */
  getMemberStatistics(workspaceId: string): Observable<{
    total_members: number;
    active_members: number;
    by_role: { [key in WorkspaceMemberRole]: number };
    by_status: { [key in MemberStatus]: number };
    recent_joins: number;
    recent_activity: Array<{
      date: string;
      joined: number;
      left: number;
      active: number;
    }>;
  }> {
    return this.http.get<{
      total_members: number;
      active_members: number;
      by_role: { [key in WorkspaceMemberRole]: number };
      by_status: { [key in MemberStatus]: number };
      recent_joins: number;
      recent_activity: Array<{
        date: string;
        joined: number;
        left: number;
        active: number;
      }>;
    }>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/statistics`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Export members data
   */
  exportMembers(format: 'CSV' | 'EXCEL' = 'CSV', filters?: MemberFilters): Observable<Blob> {
    const currentWorkspace = JSON.parse(localStorage.getItem('selected_workspace') || '{}');
    const workspaceId = currentWorkspace?.id;

    if (!workspaceId) {
      return throwError(() => new Error('No current workspace selected'));
    }

    let params = new HttpParams().set('format', format);

    if (filters) {
      if (filters.role && filters.role !== 'ALL') params = params.set('role', filters.role);
      if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
      if (filters.department) params = params.set('department', filters.department);
      if (filters.search_query) params = params.set('search_query', filters.search_query);
    }

    return this.http.get(`${this.WORKSPACE_API_URL}/${workspaceId}/members/export`, {
      params,
      responseType: 'blob'
    }).pipe(catchError(this.handleError));
  }

  /**
   * Get available departments in workspace
   */
  getDepartments(workspaceId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/departments`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Update member status (activate/deactivate/suspend)
   */
  updateMemberStatus(memberId: string, status: MemberStatus, reason?: string): Observable<WorkspaceMember> {
    const currentWorkspace = JSON.parse(localStorage.getItem('selected_workspace') || '{}');
    const workspaceId = currentWorkspace?.id;

    if (!workspaceId) {
      return throwError(() => new Error('No current workspace selected'));
    }

    const body = {
      status,
      reason
    };

    return this.http.patch<WorkspaceMember>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}/status`, body)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Workspace Member Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    // Extract error message from API response structure
    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    return throwError(() => new Error(errorMessage));
  }
}
