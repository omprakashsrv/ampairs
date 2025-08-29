import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {
  MemberFilters,
  MemberSortOptions,
  MemberStatus,
  PagedMemberResponse,
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

  constructor(private http: HttpClient) {
  }

  /**
   * Get paginated list of workspace members
   */
  getMembers(request: MemberSearchRequest = {}): Observable<PagedMemberResponse> {
    let params = new HttpParams();

    if (request.workspace_id) params = params.set('workspace_id', request.workspace_id);
    if (request.page !== undefined) params = params.set('page', request.page.toString());
    if (request.size !== undefined) params = params.set('size', request.size.toString());
    if (request.sort_by) params = params.set('sort_by', request.sort_by);
    if (request.sort_direction) params = params.set('sort_direction', request.sort_direction);
    if (request.role && request.role !== 'ALL') params = params.set('role', request.role);
    if (request.status && request.status !== 'ALL') params = params.set('status', request.status);
    if (request.department) params = params.set('department', request.department);
    if (request.search_query) params = params.set('search_query', request.search_query);

    return this.http.get<ApiResponse<PagedMemberResponse>>(`${this.MEMBER_API_URL}`, {params})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Get member details by ID
   */
  getMemberById(memberId: string): Observable<WorkspaceMember> {
    return this.http.get<ApiResponse<WorkspaceMember>>(`${this.MEMBER_API_URL}/${memberId}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Get current user's role and permissions in workspace
   */
  getCurrentUserRole(workspaceId?: string): Observable<UserRoleResponse> {
    let params = new HttpParams();
    if (workspaceId) params = params.set('workspace_id', workspaceId);

    return this.http.get<ApiResponse<UserRoleResponse>>(`${this.MEMBER_API_URL}/me`, {params})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Update member role and permissions
   */
  updateMember(memberId: string, updateRequest: UpdateMemberRequest): Observable<WorkspaceMember> {
    return this.http.put<ApiResponse<WorkspaceMember>>(`${this.MEMBER_API_URL}/${memberId}`, updateRequest)
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
    return this.http.put<ApiResponse<{
      updated_count: number;
      failed_updates: Array<{ member_id: string; error: string }>
    }>>(`${this.MEMBER_API_URL}/bulk`, bulkRequest)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Remove member from workspace
   */
  removeMember(memberId: string, reason?: string): Observable<{ message: string }> {
    const body = reason ? {reason} : {};
    return this.http.delete<ApiResponse<{ message: string }>>(`${this.MEMBER_API_URL}/${memberId}`, {body})
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
    const body = {
      member_ids: memberIds,
      reason: reason
    };
    return this.http.delete<ApiResponse<{
      removed_count: number;
      failed_removals: Array<{ member_id: string; error: string }>
    }>>(`${this.MEMBER_API_URL}/bulk`, {body})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Search members with advanced filtering
   */
  searchMembers(filters: MemberFilters, sortOptions?: MemberSortOptions, page = 0, size = 20): Observable<PagedMemberResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters.role && filters.role !== 'ALL') params = params.set('role', filters.role);
    if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters.department) params = params.set('department', filters.department);
    if (filters.search_query) params = params.set('search_query', filters.search_query);

    if (sortOptions) {
      params = params.set('sort_by', sortOptions.sort_by);
      params = params.set('sort_direction', sortOptions.sort_direction);
    }

    return this.http.get<ApiResponse<PagedMemberResponse>>(`${this.MEMBER_API_URL}/search`, {params})
      .pipe(
        map(response => response.data),
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
  getMemberStatistics(workspaceId?: string): Observable<{
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
    let params = new HttpParams();
    if (workspaceId) params = params.set('workspace_id', workspaceId);

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
    }>>(`${this.MEMBER_API_URL}/statistics`, {params})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Export members data
   */
  exportMembers(format: 'CSV' | 'EXCEL' = 'CSV', filters?: MemberFilters): Observable<Blob> {
    let params = new HttpParams().set('format', format);

    if (filters) {
      if (filters.role && filters.role !== 'ALL') params = params.set('role', filters.role);
      if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
      if (filters.department) params = params.set('department', filters.department);
      if (filters.search_query) params = params.set('search_query', filters.search_query);
    }

    return this.http.get(`${this.MEMBER_API_URL}/export`, {
      params,
      responseType: 'blob'
    }).pipe(catchError(this.handleError));
  }

  /**
   * Get available departments in workspace
   */
  getDepartments(workspaceId?: string): Observable<string[]> {
    let params = new HttpParams();
    if (workspaceId) params = params.set('workspace_id', workspaceId);

    return this.http.get<ApiResponse<string[]>>(`${this.MEMBER_API_URL}/departments`, {params})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Update member status (activate/deactivate/suspend)
   */
  updateMemberStatus(memberId: string, status: MemberStatus, reason?: string): Observable<WorkspaceMember> {
    const body = {
      status,
      reason
    };
    return this.http.patch<ApiResponse<WorkspaceMember>>(`${this.MEMBER_API_URL}/${memberId}/status`, body)
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

    throw new Error(errorMessage);
  }
}
