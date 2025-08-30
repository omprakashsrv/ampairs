/**
 * Comprehensive workspace member management interfaces and models
 *
 * These interfaces match the Swagger API documentation for complete
 * member management functionality including roles, permissions, and lifecycle.
 */

// ===== CORE MEMBER INTERFACES =====

export interface WorkspaceMember {
  id: string;
  user_id: string;
  workspace_id: string;
  email: string;
  name: string;
  role: WorkspaceMemberRole;
  status: MemberStatus;
  joined_at: string;
  last_activity?: string;
  permissions: string[];
  avatar_url?: string;
  phone?: string;
  department?: string;
  is_online: boolean;
}

export interface MemberListResponse {
  id: string;
  user_id: string;
  email: string;
  first_name: string;
  last_name: string;
  role: WorkspaceMemberRole;
  status: MemberStatus;
  joined_at: string;
  last_activity?: string;
  permissions: string[];
  avatar_url?: string;
  phone?: string;
  department?: string;
  is_online: boolean;
}

export interface UpdateMemberRequest {
  role?: WorkspaceMemberRole;
  custom_permissions?: string[];
  department?: string;
  reason?: string;
  notify_member?: boolean;
}

export interface PagedMemberResponse {
  content: MemberListResponse[];
  page_number: number;
  page_size: number;
  total_elements: number;
  total_pages: number;
  first: boolean;
  last: boolean;
}

export interface UserRoleResponse {
  user_id: string;
  workspace_id: string;
  current_role: WorkspaceMemberRole;
  membership_status: MemberStatus;
  joined_at: string;
  last_activity?: string;
  role_hierarchy: RoleHierarchy;
  permissions: PermissionMatrix;
  module_access: string[];
  restrictions?: { [key: string]: any };
}

// Simplified interface matching actual backend response
export interface SimpleUserRoleResponse {
  is_owner: boolean;
  is_admin: boolean;
  can_view_members: boolean;
  can_invite_members: boolean;
  can_manage_workspace: boolean;
}

// ===== ROLE AND PERMISSION TYPES =====

export type WorkspaceMemberRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'MEMBER' | 'GUEST' | 'VIEWER';

export type MemberStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED';

export interface RoleHierarchy {
  is_owner: boolean;
  is_admin: boolean;
  is_manager: boolean;
  is_member: boolean;
  is_viewer: boolean;
}

export interface PermissionMatrix {
  workspace_management: WorkspaceManagementPermissions;
  member_management: MemberManagementPermissions;
  data_operations: DataOperationsPermissions;
  reporting: ReportingPermissions;
}

export interface WorkspaceManagementPermissions {
  can_manage_workspace: boolean;
  can_view_settings: boolean;
  can_modify_settings: boolean;
  can_delete_workspace: boolean;
}

export interface MemberManagementPermissions {
  can_view_members: boolean;
  can_invite_members: boolean;
  can_manage_members: boolean;
  can_remove_members: boolean;
}

export interface DataOperationsPermissions {
  can_view_data: boolean;
  can_create_data: boolean;
  can_update_data: boolean;
  can_delete_data: boolean;
  can_export_data: boolean;
}

export interface ReportingPermissions {
  can_view_reports: boolean;
  can_create_reports: boolean;
  can_share_reports: boolean;
  can_access_analytics: boolean;
}

// ===== FILTER AND SEARCH INTERFACES =====

export interface MemberFilters {
  role: WorkspaceMemberRole | 'ALL';
  status: MemberStatus | 'ALL';
  department?: string;
  search_query?: string;
}

export interface MemberSortOptions {
  sort_by: 'joinedAt' | 'name' | 'email' | 'role' | 'lastActivity';
  sort_direction: 'asc' | 'desc';
}

// ===== ROLE CONFIGURATION =====

export const ROLE_HIERARCHY: { [key in WorkspaceMemberRole]: number } = {
  'OWNER': 6,
  'ADMIN': 5,
  'MANAGER': 4,
  'MEMBER': 3,
  'GUEST': 2,
  'VIEWER': 1
};

export const ROLE_DESCRIPTIONS: { [key in WorkspaceMemberRole]: string } = {
  'OWNER': 'Workspace owner with full control and billing access',
  'ADMIN': 'Administrative access to workspace management and member control',
  'MANAGER': 'Business operations management with limited administrative access',
  'MEMBER': 'Standard workspace participant with data creation and editing rights',
  'GUEST': 'Limited temporary access with restricted scope and duration',
  'VIEWER': 'Read-only access to workspace data and basic reporting'
};

export const ROLE_COLORS: { [key in WorkspaceMemberRole]: string } = {
  'OWNER': 'error',
  'ADMIN': 'primary',
  'MANAGER': 'secondary',
  'MEMBER': 'tertiary',
  'GUEST': 'accent',
  'VIEWER': 'basic'
};

export const DEFAULT_PERMISSIONS: { [key in WorkspaceMemberRole]: string[] } = {
  'OWNER': [
    'WORKSPACE_MANAGE', 'MEMBER_INVITE', 'MEMBER_MANAGE', 'MEMBER_DELETE',
    'DATA_MANAGE', 'REPORTS_VIEW', 'REPORTS_CREATE', 'SETTINGS_MANAGE'
  ],
  'ADMIN': [
    'WORKSPACE_MANAGE', 'MEMBER_INVITE', 'MEMBER_MANAGE',
    'DATA_MANAGE', 'REPORTS_VIEW', 'REPORTS_CREATE'
  ],
  'MANAGER': [
    'MEMBER_VIEW', 'MEMBER_INVITE', 'DATA_MANAGE', 'REPORTS_VIEW', 'REPORTS_CREATE'
  ],
  'MEMBER': [
    'MEMBER_VIEW', 'DATA_MANAGE', 'REPORTS_VIEW'
  ],
  'GUEST': [
    'DATA_VIEW'
  ],
  'VIEWER': [
    'DATA_VIEW', 'REPORTS_VIEW'
  ]
};

// ===== UTILITY FUNCTIONS =====

export function canManageRole(currentRole: WorkspaceMemberRole, targetRole: WorkspaceMemberRole): boolean {
  return ROLE_HIERARCHY[currentRole] > ROLE_HIERARCHY[targetRole];
}

export function getRoleColor(role: WorkspaceMemberRole): string {
  return ROLE_COLORS[role] || 'basic';
}

export function getRoleDescription(role: WorkspaceMemberRole): string {
  return ROLE_DESCRIPTIONS[role] || 'Unknown role';
}

export function getDefaultPermissions(role: WorkspaceMemberRole): string[] {
  return DEFAULT_PERMISSIONS[role] || [];
}
