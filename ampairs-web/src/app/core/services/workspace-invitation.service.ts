import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {
  AcceptInvitationRequest,
  BulkInvitationRequest,
  BulkInvitationResponse,
  CreateInvitationRequest,
  DeliveryStatus,
  InvitationFilters,
  InvitationResponse,
  InvitationSortOptions,
  InvitationStatistics,
  InvitationStatus,
  PagedInvitationResponse,
  PublicInvitationDetails,
  WorkspaceInvitation,
  WorkspaceMemberRole
} from '../models/workspace-invitation.interface';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  timestamp: string;
  path?: string;
  trace_id?: string;
}

export interface InvitationSearchRequest {
  workspace_id?: string;
  page?: number;
  size?: number;
  sort_by?: string;
  sort_direction?: string;
  status?: InvitationStatus | 'ALL';
  role?: WorkspaceMemberRole | 'ALL';
  delivery_status?: DeliveryStatus | 'ALL';
  search_query?: string;
  start_date?: string;
  end_date?: string;
}

export interface ResendInvitationRequest {
  custom_message?: string;
  extend_expiry?: boolean;
  new_expiry_days?: number;
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceInvitationService {
  private readonly WORKSPACE_API_URL = `${environment.apiBaseUrl}/workspace/v1`;
  private readonly GLOBAL_INVITATION_API_URL = `${environment.apiBaseUrl}/workspace/v1/invitations`;

  constructor(private http: HttpClient) {
  }

  /**
   * Get paginated list of workspace invitations
   */
  getInvitations(workspaceId: string, request: InvitationSearchRequest = {}): Observable<PagedInvitationResponse> {
    let params = new HttpParams();

    if (request.page !== undefined) params = params.set('page', request.page.toString());
    if (request.size !== undefined) params = params.set('size', request.size.toString());
    if (request.sort_by) params = params.set('sortBy', request.sort_by);
    if (request.sort_direction) params = params.set('sortDir', request.sort_direction);
    if (request.status && request.status !== 'ALL') params = params.set('status', request.status);
    if (request.role && request.role !== 'ALL') params = params.set('role', request.role);
    if (request.delivery_status && request.delivery_status !== 'ALL') params = params.set('delivery_status', request.delivery_status);
    if (request.search_query) params = params.set('search_query', request.search_query);
    if (request.start_date) params = params.set('start_date', request.start_date);
    if (request.end_date) params = params.set('end_date', request.end_date);

    return this.http.get<PagedInvitationResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations`, {params})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Get invitation details by ID
   */
  getInvitationById(workspaceId: string, invitationId: string): Observable<WorkspaceInvitation> {
    return this.http.get<ApiResponse<WorkspaceInvitation>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/${invitationId}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Create a new workspace invitation
   */
  createInvitation(workspaceId: string, invitation: CreateInvitationRequest): Observable<InvitationResponse> {
    return this.http.post<ApiResponse<InvitationResponse>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations`, invitation)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Create multiple invitations in bulk
   */
  createBulkInvitations(workspaceId: string, bulkRequest: BulkInvitationRequest): Observable<BulkInvitationResponse> {
    return this.http.post<ApiResponse<BulkInvitationResponse>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/bulk`, bulkRequest)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Resend invitation email
   */
  resendInvitation(workspaceId: string, invitationId: string, request?: ResendInvitationRequest): Observable<{
    message: string;
    delivery_status: string
  }> {
    const body = request || {};
    return this.http.post<ApiResponse<{
      message: string;
      delivery_status: string
    }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/${invitationId}/resend`, body)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Cancel invitation
   */
  cancelInvitation(workspaceId: string, invitationId: string, reason?: string): Observable<{ message: string }> {
    const body = reason ? {reason} : {};
    return this.http.delete<ApiResponse<{ message: string }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/${invitationId}`, {body})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Bulk cancel multiple invitations
   */
  bulkCancelInvitations(workspaceId: string, invitationIds: string[], reason?: string): Observable<{
    cancelled_count: number;
    failed_cancellations: Array<{ invitation_id: string; error: string }>
  }> {
    const body = {
      invitation_ids: invitationIds,
      reason: reason
    };
    return this.http.delete<ApiResponse<{
      cancelled_count: number;
      failed_cancellations: Array<{ invitation_id: string; error: string }>
    }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/bulk`, {body})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Delete invitation permanently
   */
  deleteInvitation(workspaceId: string, invitationId: string): Observable<{ message: string }> {
    return this.http.delete<ApiResponse<{ message: string }>>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/${invitationId}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Get public invitation details (no authentication required)
   */
  getPublicInvitationDetails(invitationCode: string): Observable<PublicInvitationDetails> {
    return this.http.get<PublicInvitationDetails>(`${this.GLOBAL_INVITATION_API_URL}/public/${invitationCode}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Accept invitation (public endpoint)
   */
  acceptInvitation(token: string): Observable<{
    message: string;
    workspace_id: string;
    member_id: string
  }> {
    return this.http.post<{
      message: string;
      workspace_id: string;
      member_id: string
    }>(`${this.GLOBAL_INVITATION_API_URL}/${token}/accept`, {})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Decline invitation (public endpoint)
   */
  declineInvitation(invitationCode: string, reason?: string): Observable<{ message: string }> {
    const body = {
      invitation_code: invitationCode,
      reason: reason
    };
    return this.http.post<ApiResponse<{ message: string }>>(`${this.GLOBAL_INVITATION_API_URL}/decline`, body)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Search invitations with advanced filtering
   */
  searchInvitations(workspaceId: string, filters: InvitationFilters, sortOptions?: InvitationSortOptions, page = 0, size = 20): Observable<PagedInvitationResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters.role && filters.role !== 'ALL') params = params.set('role', filters.role);
    if (filters.delivery_status && filters.delivery_status !== 'ALL') params = params.set('delivery_status', filters.delivery_status);
    if (filters.search_query) params = params.set('search_query', filters.search_query);

    if (filters.date_range) {
      params = params.set('start_date', filters.date_range.start_date);
      params = params.set('end_date', filters.date_range.end_date);
    }

    if (sortOptions) {
      params = params.set('sortBy', sortOptions.sort_by);
      params = params.set('sortDir', sortOptions.sort_direction);
    }

    return this.http.get<PagedInvitationResponse>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/search`, {params})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Get invitation statistics
   */
  getInvitationStatistics(workspaceId: string): Observable<InvitationStatistics> {
    return this.http.get<InvitationStatistics>(`${this.WORKSPACE_API_URL}/${workspaceId}/invitations/statistics`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Export invitations data
   */
  exportInvitations(format: 'CSV' | 'EXCEL' = 'CSV', filters?: InvitationFilters): Observable<Blob> {
    let params = new HttpParams().set('format', format);

    if (filters) {
      if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);
      if (filters.role && filters.role !== 'ALL') params = params.set('role', filters.role);
      if (filters.delivery_status && filters.delivery_status !== 'ALL') params = params.set('delivery_status', filters.delivery_status);
      if (filters.search_query) params = params.set('search_query', filters.search_query);

      if (filters.date_range) {
        params = params.set('start_date', filters.date_range.start_date);
        params = params.set('end_date', filters.date_range.end_date);
      }
    }

    return this.http.get(`${this.GLOBAL_INVITATION_API_URL}/export`, {
      params,
      responseType: 'blob'
    }).pipe(catchError(this.handleError));
  }

  /**
   * Get invitation delivery tracking
   */
  getDeliveryTracking(invitationId: string): Observable<{
    invitation_id: string;
    delivery_attempts: Array<{
      attempt_number: number;
      attempted_at: string;
      status: DeliveryStatus;
      error_message?: string;
      provider_response?: string;
    }>;
    current_status: DeliveryStatus;
    last_attempt: string;
  }> {
    return this.http.get<ApiResponse<{
      invitation_id: string;
      delivery_attempts: Array<{
        attempt_number: number;
        attempted_at: string;
        status: DeliveryStatus;
        error_message?: string;
        provider_response?: string;
      }>;
      current_status: DeliveryStatus;
      last_attempt: string;
    }>>(`${this.GLOBAL_INVITATION_API_URL}/${invitationId}/delivery-tracking`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Update invitation expiry
   */
  updateInvitationExpiry(invitationId: string, expiryDays: number): Observable<WorkspaceInvitation> {
    const body = {expiry_days: expiryDays};
    return this.http.patch<ApiResponse<WorkspaceInvitation>>(`${this.GLOBAL_INVITATION_API_URL}/${invitationId}/expiry`, body)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Get invitation analytics for dashboard
   */
  getInvitationAnalytics(workspaceId?: string, days = 30): Observable<{
    total_sent: number;
    acceptance_rate: number;
    average_response_time_hours: number;
    daily_stats: Array<{
      date: string;
      sent: number;
      accepted: number;
      declined: number;
      expired: number;
    }>;
    role_distribution: { [key in WorkspaceMemberRole]: number };
    status_breakdown: { [key in InvitationStatus]: number };
    delivery_performance: { [key in DeliveryStatus]: number };
  }> {
    let params = new HttpParams().set('days', days.toString());
    if (workspaceId) params = params.set('workspace_id', workspaceId);

    return this.http.get<ApiResponse<{
      total_sent: number;
      acceptance_rate: number;
      average_response_time_hours: number;
      daily_stats: Array<{
        date: string;
        sent: number;
        accepted: number;
        declined: number;
        expired: number;
      }>;
      role_distribution: { [key in WorkspaceMemberRole]: number };
      status_breakdown: { [key in InvitationStatus]: number };
      delivery_performance: { [key in DeliveryStatus]: number };
    }>>(`${this.GLOBAL_INVITATION_API_URL}/analytics`, {params})
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Workspace Invitation Service Error:', error);
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
