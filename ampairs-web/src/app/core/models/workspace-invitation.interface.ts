/**
 * Comprehensive workspace invitation management interfaces and models
 *
 * These interfaces match the Swagger API documentation for complete
 * invitation management functionality including lifecycle, delivery tracking, and acceptance.
 */

// ===== CORE INVITATION INTERFACES =====

export interface WorkspaceInvitation {
  id: string;
  workspace_id: string;
  inviter_user_id: string;
  inviter_name: string;
  email: string;
  role: WorkspaceMemberRole;
  status: InvitationStatus;
  created_at: string;
  expires_at: string;
  accepted_at?: string;
  declined_at?: string;
  cancelled_at?: string;
  reminder_count: number;
  last_reminder_sent?: string;
  invitation_code: string;
  custom_message?: string;
  delivery_status: DeliveryStatus;
  delivery_attempts: number;
  last_delivery_attempt?: string;
  delivery_error?: string;
}

export interface InvitationListResponse {
  id: string;
  workspace_id: string;
  inviter_name: string;
  email: string;
  role: WorkspaceMemberRole;
  status: InvitationStatus;
  created_at: string;
  expires_at: string;
  reminder_count: number;
  delivery_status: DeliveryStatus;
  custom_message?: string;
}

export interface CreateInvitationRequest {
  email: string;
  role: WorkspaceMemberRole;
  custom_message?: string;
  expires_in_days?: number;
}

export interface BulkInvitationRequest {
  invitations: Array<{
    email: string;
    role: WorkspaceMemberRole;
    custom_message?: string;
  }>;
  expires_in_days?: number;
  send_immediately?: boolean;
}

export interface InvitationResponse {
  id: string;
  workspace_id: string;
  email: string;
  role: WorkspaceMemberRole;
  status: InvitationStatus;
  invitation_code: string;
  created_at: string;
  expires_at: string;
  invitation_url: string;
}

export interface BulkInvitationResponse {
  successful_invitations: InvitationResponse[];
  failed_invitations: Array<{
    email: string;
    error: string;
    error_code: string;
  }>;
  total_sent: number;
  total_failed: number;
}

export interface AcceptInvitationRequest {
  invitation_code: string;
  user_name?: string;
  phone?: string;
  accept_terms?: boolean;
}

export interface PublicInvitationDetails {
  id: string;
  workspace_name: string;
  workspace_type: string;
  inviter_name: string;
  role: WorkspaceMemberRole;
  expires_at: string;
  is_expired: boolean;
  custom_message?: string;
  workspace_avatar_url?: string;
}

export interface PagedInvitationResponse {
  content: InvitationListResponse[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
  is_first: boolean;
  is_last: boolean;
}

// ===== INVITATION STATUS AND DELIVERY TYPES =====

export type InvitationStatus =
  | 'PENDING'
  | 'SENT'
  | 'DELIVERED'
  | 'OPENED'
  | 'ACCEPTED'
  | 'DECLINED'
  | 'EXPIRED'
  | 'CANCELLED'
  | 'FAILED';

export type DeliveryStatus =
  | 'PENDING'
  | 'SENDING'
  | 'SENT'
  | 'DELIVERED'
  | 'BOUNCED'
  | 'FAILED'
  | 'BLOCKED';

export type WorkspaceMemberRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'MEMBER' | 'GUEST' | 'VIEWER';

// ===== FILTER AND SEARCH INTERFACES =====

export interface InvitationFilters {
  status: InvitationStatus | 'ALL';
  role: WorkspaceMemberRole | 'ALL';
  delivery_status: DeliveryStatus | 'ALL';
  search_query?: string;
  date_range?: {
    start_date: string;
    end_date: string;
  };
}

export interface InvitationSortOptions {
  sort_by: 'createdAt' | 'email' | 'role' | 'status' | 'expiresAt';
  sort_direction: 'asc' | 'desc';
}

// ===== INVITATION STATISTICS =====

export interface InvitationStatistics {
  total_sent: number;
  total_pending: number;
  total_accepted: number;
  total_declined: number;
  total_expired: number;
  total_cancelled: number;
  acceptance_rate: number;
  average_response_time_hours: number;
  by_role: { [key in WorkspaceMemberRole]: number };
  by_status: { [key in InvitationStatus]: number };
  by_delivery_status: { [key in DeliveryStatus]: number };
  recent_activity: Array<{
    date: string;
    sent: number;
    accepted: number;
    declined: number;
  }>;
}

// ===== CONFIGURATION AND TEMPLATES =====

export const INVITATION_STATUS_COLORS: { [key in InvitationStatus]: string } = {
  'PENDING': 'accent',
  'SENT': 'primary',
  'DELIVERED': 'primary',
  'OPENED': 'secondary',
  'ACCEPTED': 'success',
  'DECLINED': 'warn',
  'EXPIRED': 'basic',
  'CANCELLED': 'basic',
  'FAILED': 'error'
};

export const INVITATION_STATUS_DESCRIPTIONS: { [key in InvitationStatus]: string } = {
  'PENDING': 'Invitation created but not yet sent',
  'SENT': 'Invitation email has been sent',
  'DELIVERED': 'Email successfully delivered to recipient',
  'OPENED': 'Recipient has opened the invitation email',
  'ACCEPTED': 'Invitation accepted and user joined workspace',
  'DECLINED': 'Invitation declined by recipient',
  'EXPIRED': 'Invitation expired without response',
  'CANCELLED': 'Invitation cancelled by sender',
  'FAILED': 'Failed to send invitation due to error'
};

export const DELIVERY_STATUS_COLORS: { [key in DeliveryStatus]: string } = {
  'PENDING': 'accent',
  'SENDING': 'primary',
  'SENT': 'primary',
  'DELIVERED': 'success',
  'BOUNCED': 'warn',
  'FAILED': 'error',
  'BLOCKED': 'error'
};

export const DELIVERY_STATUS_DESCRIPTIONS: { [key in DeliveryStatus]: string } = {
  'PENDING': 'Email queued for delivery',
  'SENDING': 'Email is being sent',
  'SENT': 'Email sent to email service provider',
  'DELIVERED': 'Email successfully delivered to inbox',
  'BOUNCED': 'Email bounced back due to invalid address',
  'FAILED': 'Email delivery failed due to technical error',
  'BLOCKED': 'Email blocked by recipient email provider'
};

export const DEFAULT_INVITATION_EXPIRY_DAYS = 7;
export const MAX_INVITATION_EXPIRY_DAYS = 30;
export const MAX_REMINDER_COUNT = 3;
export const BULK_INVITATION_LIMIT = 50;

// ===== UTILITY FUNCTIONS =====

export function getInvitationStatusColor(status: InvitationStatus): string {
  return INVITATION_STATUS_COLORS[status] || 'basic';
}

export function getInvitationStatusDescription(status: InvitationStatus): string {
  return INVITATION_STATUS_DESCRIPTIONS[status] || 'Unknown status';
}

export function getDeliveryStatusColor(status: DeliveryStatus): string {
  return DELIVERY_STATUS_COLORS[status] || 'basic';
}

export function getDeliveryStatusDescription(status: DeliveryStatus): string {
  return DELIVERY_STATUS_DESCRIPTIONS[status] || 'Unknown delivery status';
}

export function isInvitationExpired(expiresAt: string): boolean {
  return new Date() > new Date(expiresAt);
}

export function getInvitationTimeRemaining(expiresAt: string): string {
  const now = new Date();
  const expiry = new Date(expiresAt);
  const diffMs = expiry.getTime() - now.getTime();

  if (diffMs <= 0) {
    return 'Expired';
  }

  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

  if (diffDays > 0) {
    return `${diffDays} day${diffDays > 1 ? 's' : ''} remaining`;
  } else if (diffHours > 0) {
    return `${diffHours} hour${diffHours > 1 ? 's' : ''} remaining`;
  } else {
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} remaining`;
  }
}

export function canResendInvitation(status: InvitationStatus, reminderCount: number): boolean {
  const resendableStatuses: InvitationStatus[] = ['PENDING', 'SENT', 'DELIVERED', 'OPENED'];
  return resendableStatuses.includes(status) && reminderCount < MAX_REMINDER_COUNT;
}

export function canCancelInvitation(status: InvitationStatus): boolean {
  const cancellableStatuses: InvitationStatus[] = ['PENDING', 'SENT', 'DELIVERED', 'OPENED'];
  return cancellableStatuses.includes(status);
}

export function getInvitationUrl(baseUrl: string, invitationCode: string): string {
  return `${baseUrl}/workspace/invitation/accept/${invitationCode}`;
}
