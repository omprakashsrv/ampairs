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

    return this.http.get<ApiResponse<any>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members`, {params})
      .pipe(
        map(response => {
          const data = response.data;
          // Map backend response format to frontend expected format
          return {
            content: data.content || [],
            page_number: data.page_number || 0,
            page_size: data.page_size || 20,
            total_elements: data.total_elements || 0,
            total_pages: data.total_pages || 0,
            first: data.first || false,
            last: data.last || false
          } as PagedMemberResponse;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get member details by ID
   */
  getMemberById(workspaceId: string, memberId: string): Observable<WorkspaceMember> {
    return this.http.get<ApiResponse<WorkspaceMember>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}`)
      .pipe(
        map(response => response.data),
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

    return this.http.get<any>(url)
      .pipe(
        map(response => {
          console.log('Service received full response:', response);

          // Handle both wrapped and direct response formats
          let data: SimpleUserRoleResponse;

          if (response && typeof response === 'object') {
            // Check if it's wrapped in ApiResponse format
            if (response.success !== undefined && response.data) {
              data = response.data;
              console.log('Using wrapped format, extracted data:', data);
            }
            // Check if it's direct permission object
            else if (response.is_owner !== undefined || response.can_view_members !== undefined) {
              data = response;
              console.log('Using direct format, data:', data);
            } else {
              console.error('Unknown response structure:', response);
              throw new Error('Unknown response structure');
            }
          } else {
            console.error('Invalid response type:', typeof response, response);
            throw new Error('Invalid response type');
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
    return this.http.put<ApiResponse<WorkspaceMember>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}`, updateRequest)
      .pipe(
        map(response => response.data),
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

    return this.http.put<ApiResponse<{
      updated_count: number;
      failed_updates: Array<{ member_id: string; error: string }>
    }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/bulk`, bulkRequest)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Remove member from workspace
   */
  removeMember(workspaceId: string, memberId: string, reason?: string): Observable<{ message: string }> {
    const body = reason ? {reason} : {};
    return this.http.delete<ApiResponse<{
      message: string
    }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}`, {body})
      .pipe(
        map(response => response.data),
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

    return this.http.delete<ApiResponse<{
      removed_count: number;
      failed_removals: Array<{ member_id: string; error: string }>
    }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/bulk`, {body})
      .pipe(
        map(response => response.data),
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

    return this.http.get<PagedMemberResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/search`, {params})
      .pipe(
        map(data => {
          // Map backend response format to frontend expected format
          return data;
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

    return this.http.get<ApiResponse<{
      content: MemberActivityLog[];
      total_elements: number
    }>>(`${this.MEMBER_API_URL}/${memberId}/activity`, {params})
      .pipe(
        map(response => response.data),
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
    return this.http.get<ApiResponse<{
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
    }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/statistics`)
      .pipe(
        map(response => response.data),
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
    return this.http.get<ApiResponse<string[]>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/departments`)
      .pipe(
        map(response => response.data),
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

    return this.http.patch<ApiResponse<WorkspaceMember>>(`${this.WORKSPACE_API_URL}/${workspaceId}/members/${memberId}/status`, body)
      .pipe(
        map(response => response.data),
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
