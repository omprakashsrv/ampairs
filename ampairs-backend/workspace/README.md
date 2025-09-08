# Workspace Module

## Overview

The Workspace module is the foundational multi-tenant management system for Ampairs, providing comprehensive business
workspace management with advanced role-based access control, team organization, and permission management. It enables
complete tenant isolation, hierarchical user management, and scalable business operations across multiple workspaces.

## Architecture

### Package Structure

```
com.ampairs.workspace/
├── config/                # Configuration and security setup
│   ├── Constants.kt       # Module-specific constants
│   ├── UserDetailConfiguration.kt # User service integration
│   └── WorkspaceSecurityConfig.kt # Security configuration
├── controller/            # REST API endpoints
│   ├── WorkspaceController.kt # Core workspace operations
│   ├── WorkspaceMemberController.kt # Member management operations
│   ├── WorkspaceInvitationController.kt # Member invitation management
│   ├── WorkspaceTeamController.kt # Team management operations
│   └── WorkspaceModuleController.kt # Module management operations
├── exception/             # Workspace exception handling
├── filter/                # Request processing filters
│   └── SessionUserFilter.kt # Session context management
├── model/                 # Domain entities and DTOs
│   ├── dto/               # Data Transfer Objects
│   └── enums/             # Workspace enumerations
├── repository/            # Data access layer
├── security/              # Permission and authorization
└── service/               # Business logic services
```

## Key Components

### Controllers

- **`WorkspaceController.kt`** - Core workspace CRUD operations, workspace discovery, and configuration management
- **`WorkspaceMemberController.kt`** - Comprehensive member management, role assignments, permissions, and member
  lifecycle operations
- **`WorkspaceInvitationController.kt`** - Member invitation lifecycle, bulk invitations, and invitation statistics
- **`WorkspaceTeamController.kt`** - Team creation, member assignment, and team-based access control
- **`WorkspaceModuleController.kt`** - Business module management, installation, configuration, and lifecycle operations

### Models

#### Core Entities

- **`Workspace.kt`** - Primary workspace entity with business profile, settings, and tenant configuration
- **`WorkspaceMember.kt`** - User membership within workspaces with roles, permissions, and team associations
- **`WorkspaceTeam.kt`** - Team organization with department-level access control and member management
- **`WorkspaceSettings.kt`** - Workspace-specific configuration and preferences
- **`WorkspaceModule.kt`** - Enabled business modules and their configurations
- **`WorkspaceActivity.kt`** - Audit trail and activity tracking
- **`MasterModule.kt`** - Available business modules in the system

#### Key DTOs

**Workspace Management:**

- **`WorkspaceRequest.kt/WorkspaceResponse.kt`** - Workspace CRUD operations
- **`WorkspaceConfigurationResponse.kt`** - Complete workspace configuration
- **`ModuleManagementRequest.kt/Response.kt`** - Module activation and settings

**Team Management:**

- **`CreateTeamRequest.kt/UpdateTeamRequest.kt`** - Team lifecycle management
- **`TeamResponse.kt/TeamListResponse.kt`** - Team information and member summaries
- **`AddTeamMembersRequest.kt/RemoveTeamMembersRequest.kt`** - Team membership operations

**Member & Invitation Management:**

- **`InvitationRequest.kt/InvitationResponse.kt`** - Member invitation workflow
- **`MemberResponse.kt`** - Member profile and permissions
- **`SettingsRequest.kt/Response.kt`** - Workspace settings management

#### Enumerations

- **`WorkspaceRole.kt`** - Hierarchical user roles (OWNER → ADMIN → MANAGER → MEMBER → GUEST → VIEWER)
- **`WorkspaceStatus.kt`** - Workspace lifecycle states
- **`WorkspaceType.kt`** - Business workspace categories
- **`SubscriptionPlan.kt`** - Subscription tier definitions
- **`InvitationStatus.kt`** - Invitation lifecycle tracking
- **`ModuleStatus.kt/ModuleCategory.kt`** - Module management enums

### Services

- **`WorkspaceService.kt`** - Core workspace management, member operations, and multi-tenant context
- **`WorkspaceMemberService.kt`** - Comprehensive member lifecycle management, role assignments, permissions, and member
  analytics
- **`WorkspaceTeamService.kt`** - Team creation, member assignment, and team-based permissions
- **`WorkspaceInvitationService.kt`** - Member invitation workflow, bulk operations, and acceptance tracking
- **`WorkspaceModuleService.kt`** - Business module discovery, installation, configuration, and lifecycle management
- **`WorkspaceSettingsService.kt`** - Workspace configuration, preferences, and customization
- **`WorkspaceActivityService.kt`** - Activity logging, audit trails, and workspace analytics
- **`WorkspaceNotificationService.kt`** - Notification system for workspace events
- **`UserDetailProvider.kt`** - User information integration and profile management

### Repositories

- **`WorkspaceRepository.kt`** - Workspace entity data access and tenant queries
- **`WorkspaceMemberRepository.kt`** - Member management and role-based queries
- **`WorkspaceTeamRepository.kt`** - Team management and member association queries
- **`WorkspaceInvitationRepository.kt`** - Invitation tracking and lifecycle management
- **`WorkspaceModuleRepository.kt`** - Workspace module installation and configuration storage
- **`MasterModuleRepository.kt`** - Master module catalog access and statistics
- **`WorkspaceSettingsRepository.kt`** - Workspace configuration persistence
- **`WorkspaceActivityRepository.kt`** - Activity logging and audit trail storage

### Security & Configuration

- **`SessionUserFilter.kt`** - HTTP filter for multi-tenant session context management
- **`WorkspaceSecurityConfig.kt`** - Security configuration and authorization rules
- **`WorkspacePermission.kt`** - Fine-grained permission definitions
- **`Constants.kt`** - Module constants and configuration values

## Key Features

### 🏢 Multi-tenant Workspace Management

- **Complete Business Profiles**: Company information, registration details (GST, PAN), contact management
- **Workspace Types**: STARTUP, BUSINESS, ENTERPRISE, PERSONAL with tailored feature sets
- **Subscription Management**: Plan-based feature access and billing integration
- **Geographic Support**: Location-based organization, address standardization, and regional settings

### 🧩 Advanced Module Management System

- **Master Module Catalog**: Centralized registry of 50+ business modules across 12 categories
- **Dynamic Installation**: Install/uninstall modules with dependency checking and conflict resolution
- **Module Categories**: Customer Management, Sales, Financial, Inventory, Project Management, Analytics, etc.
- **Configuration Management**: Per-workspace module customization with settings persistence
- **Lifecycle Operations**: Enable/disable, update, diagnose, optimize, and health monitoring
- **Usage Analytics**: Module performance tracking, user engagement metrics, and optimization recommendations
- **Dependency Management**: Automatic dependency resolution and conflict prevention
- **License Management**: Module licensing with tier-based access control and expiration tracking

### 👥 Advanced Member & Team Management

- **6-Level Role Hierarchy**: OWNER → ADMIN → MANAGER → MEMBER → GUEST → VIEWER with granular permissions
- **Member Lifecycle Management**: Complete onboarding, role changes, status updates, and offboarding workflows
- **Advanced Member Profiles**: Contact details, departments, activity tracking, and engagement metrics
- **Team Organization**: Department-based hierarchical teams with designated leads and capacity limits
- **Bulk Operations**: Mass member updates, role changes, and removals with transaction safety
- **Advanced Search & Analytics**: Multi-criteria filtering, statistics, and data export capabilities

### 📧 Invitation & Activity Management

- **Bulk Invitations**: Mass invitation capabilities with CSV import and custom messages
- **Invitation Analytics**: Acceptance rates, pending invitations, and usage statistics
- **Complete Audit Trail**: All workspace changes with user attribution and timestamps
- **Activity Analytics**: 20+ tracked activity types with exportable logs for compliance
- **Real-time Notifications**: Workspace event notifications and alerts

## Data Model

### Core Entity Relationships

```kotlin
// Primary workspace entity with complete business profile
@Entity("workspaces")
class Workspace : BaseDomain() {
    var name: String
    var workspaceType: WorkspaceType
    var subscriptionPlan: SubscriptionPlan
    var businessProfile: BusinessProfile // GST, PAN, etc.
    var contactDetails: ContactDetails
    var address: Address
    var settings: WorkspaceSettings
    var status: WorkspaceStatus
    var enabledModuleIds: Set<String> // JSON array
}

// Member with role hierarchy and team associations
@Entity("workspace_members")
class WorkspaceMember : BaseDomain() {
    var workspaceId: String
    var userId: String
    var role: WorkspaceRole // Hierarchical enum
    var permissions: Set<WorkspacePermission> // JSON array
    var teamIds: Set<String> // JSON array of team memberships
    var primaryTeamId: String? // Primary team designation
    var isActive: Boolean
    var invitationDetails: InvitationInfo
    var activityTracking: ActivityInfo
}

// Team organization with department-level access control
@Entity("workspace_teams")
class WorkspaceTeam : BaseDomain() {
    var workspaceId: String
    var teamCode: String // Unique within workspace
    var name: String
    var department: String?
    var permissions: Set<WorkspacePermission> // Team-specific permissions
    var teamLeadId: String?
    var maxMembers: Int?
    var isActive: Boolean
}

// Comprehensive invitation management
@Entity("workspace_invitations")
class WorkspaceInvitation : BaseDomain() {
    var workspaceId: String
    var email: String
    var role: WorkspaceRole
    var invitedBy: String
    var status: InvitationStatus
    var expiresAt: LocalDateTime
    var acceptedAt: LocalDateTime?
    var customMessage: String?
}

// Module management system
@Entity("workspace_modules")
class WorkspaceModule : BaseDomain() {
    var workspaceId: String
    var masterModule: MasterModule // Reference to catalog
    var status: WorkspaceModuleStatus
    var enabled: Boolean
    var installedVersion: String
    var installedAt: LocalDateTime
    var settings: ModuleSettings // JSON configuration
    var usageMetrics: ModuleUsageMetrics // Analytics data
    var licenseInfo: String?
    var displayOrder: Int
}

// Master module catalog
@Entity("master_modules")
class MasterModule : BaseDomain() {
    var moduleCode: String // Unique identifier
    var name: String
    var category: ModuleCategory
    var version: String
    var configuration: ModuleConfiguration // Dependencies, features
    var requiredTier: SubscriptionPlan
    var complexity: ModuleComplexity
    var rating: Double
    var installCount: Int
    var featured: Boolean
}
```

## API Endpoints

The Workspace module provides comprehensive REST APIs organized by functional area:

### 🏢 Workspace Management (`/workspace/v1/workspaces`)

#### List User Workspaces
```http
GET /workspace/v1/workspaces
Authorization: Bearer <access-token>
Query Parameters:
  - page: Page number (0-based)
  - size: Page size (default: 20)
  - sort: Sort field (name, createdAt, etc.)
  - direction: Sort direction (asc/desc)
  - query: Search text
  - type: Workspace type filter
  - status: Status filter
```

#### Get Workspace Details

```http
GET /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Create Workspace
```http
POST /workspace/v1/workspaces
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "ACME Corporation",
  "description": "Software development company",
  "workspace_type": "BUSINESS",
  "subscription_plan": "ENTERPRISE",
  "business_profile": {
    "business_type": "PRIVATE_LIMITED",
    "gst_number": "29AABCU9603R1ZX",
    "pan_number": "AABCU9603R"
  },
  "contact_details": {
    "email": "info@acme.com",
    "phone": "+91-9876543210",
    "website": "https://acme.com"
  },
  "address": {
    "city": "Bangalore",
    "state": "Karnataka",
    "country": "India",
    "pincode": "560001"
  }
}
```

#### Update Workspace
```http
PUT /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "name": "Updated Workspace Name",
  "description": "Updated description"
}
```

**Permission Required:** WORKSPACE_MANAGE

#### Archive Workspace

```http
POST /workspace/v1/workspaces/{workspaceId}/archive
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** WORKSPACE_DELETE

#### Delete Workspace

```http
DELETE /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** WORKSPACE_DELETE

#### Permanently Delete Workspace
```http
DELETE /workspace/v1/workspaces/{workspaceId}/permanent
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** WORKSPACE_DELETE

### 👥 Team Management (`/workspace/v1/{workspaceId}/teams`)

#### Create Team
```http
POST /workspace/v1/{workspaceId}/teams
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "name": "Sales Team",
  "description": "Customer acquisition and sales management",
  "team_code": "SALES001",
  "department": "Sales",
  "permissions": ["VIEW_CUSTOMERS", "MANAGE_ORDERS"],
  "team_lead_id": "MBR_JANE_DOE_123",
  "max_members": 15
}
```

#### Add Team Members
```http
POST /workspace/v1/{workspaceId}/teams/{teamId}/members
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "member_ids": ["MBR_001", "MBR_002"],
  "set_as_primary": false
}
```

#### List Teams

```http
GET /workspace/v1/{workspaceId}/teams
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - page: Page number (0-based)
  - size: Page size (default: 20)
  - search: Search text
  - department: Department filter
  - status: Status filter (ACTIVE/INACTIVE)
```

#### Get Team Details

```http
GET /workspace/v1/{workspaceId}/teams/{teamId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Update Team

```http
PUT /workspace/v1/{workspaceId}/teams/{teamId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "name": "Updated Team Name",
  "description": "Updated description",
  "department": "Updated Department",
  "team_lead_id": "MBR_NEW_LEAD_123"
}
```

**Permission Required:** MANAGE_TEAMS

#### Delete Team

```http
DELETE /workspace/v1/{workspaceId}/teams/{teamId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** MANAGE_TEAMS

#### Remove Team Members

```http
POST /workspace/v1/{workspaceId}/teams/{teamId}/members/remove
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "member_ids": ["MBR_001", "MBR_002"]
}
```

**Permission Required:** MANAGE_TEAM_MEMBERS

#### Update Team Member

```http
PUT /workspace/v1/{workspaceId}/teams/{teamId}/members/{memberId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "role": "TEAM_LEAD",
  "set_as_primary": true
}
```

**Permission Required:** MANAGE_TEAM_MEMBERS

### 📧 Invitation Management (`/workspace/v1/{workspaceId}/invitations`)

#### List Invitations

```http
GET /workspace/v1/{workspaceId}/invitations
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - page: Page number (0-based)
  - size: Page size (default: 20)
  - status: Status filter (PENDING, ACCEPTED, EXPIRED, CANCELLED)
  - email: Email filter
  - role: Role filter
```

#### Create Invitation

```http
POST /workspace/v1/{workspaceId}/invitations
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "email": "user@example.com",
  "role": "MEMBER",
  "expires_in": "7d",
  "custom_message": "Join our workspace!"
}
```

#### Bulk Invitations
```http
POST /workspace/v1/{workspaceId}/invitations/bulk
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "invitations": [
    {"email": "user1@example.com", "role": "MEMBER"},
    {"email": "user2@example.com", "role": "MANAGER"}
  ],
  "default_message": "Join our team!",
  "expires_in": "7d"
}
```

#### Accept Invitation

```http
POST /workspace/v1/invitations/{token}/accept
Authorization: Bearer <access-token>
Content-Type: application/json
```

**Note:** Public endpoint - no workspace context needed

#### Resend Invitation

```http
POST /workspace/v1/{workspaceId}/invitations/{invitationId}/resend
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Cancel Invitation

```http
DELETE /workspace/v1/{workspaceId}/invitations/{invitationId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Search Invitations

```http
GET /workspace/v1/{workspaceId}/invitations/search
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - query: Search text (email, role)
  - status: Status filter
  - role: Role filter
  - page: Page number
  - size: Page size
```

#### Invitation Statistics

```http
GET /workspace/v1/{workspaceId}/invitations/statistics
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Bulk Cancel Invitations

```http
DELETE /workspace/v1/{workspaceId}/invitations/bulk
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "invitation_ids": ["INV_001", "INV_002"]
}
```

#### Bulk Resend Invitations

```http
POST /workspace/v1/{workspaceId}/invitations/bulk-resend
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "invitation_ids": ["INV_001", "INV_002"]
}
```

#### Export Invitations

```http
GET /workspace/v1/{workspaceId}/invitations/export
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - format: Export format (CSV, EXCEL)
  - status: Status filter
  - role: Role filter
```

### 👥 Member Management (`/workspace/v1/{workspaceId}/members`)

#### List Members

```http
GET /workspace/v1/{workspaceId}/members
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - page: Page number (0-based)
  - size: Page size (default: 20)
  - sortBy: Sort field (joinedAt, name, role, lastActivity)
  - sortDir: Sort direction (asc/desc)
```

**Response:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "MBR_001_JOHN_DOE",
        "user_id": "USR_JOHN_DOE_123",
        "email": "john.doe@example.com",
        "name": "John Doe",
        "role": "ADMIN",
        "status": "ACTIVE",
        "joined_at": "2025-01-10T09:15:00Z",
        "last_activity": "2025-01-15T14:30:00Z",
        "permissions": [
          "WORKSPACE_MANAGE",
          "MEMBER_INVITE"
        ],
        "department": "Engineering",
        "is_online": true
      }
    ],
    "page": 0,
    "total_elements": 15
  }
}
```

#### Get Member Details

```http
GET /workspace/v1/{workspaceId}/members/{memberId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Update Member Role & Permissions

```http
PUT /workspace/v1/{workspaceId}/members/{memberId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "role": "MANAGER",
  "custom_permissions": ["DATA_MANAGE", "REPORTS_VIEW"],
  "department": "Sales",
  "reason": "Promotion to team lead position",
  "notify_member": true
}
```

**Permission Required:** MEMBER_MANAGE role

#### Remove Member

```http
DELETE /workspace/v1/{workspaceId}/members/{memberId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** MEMBER_DELETE role

#### Get Current User's Role & Permissions

```http
GET /workspace/v1/{workspaceId}/my-role
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Search Members

```http
GET /workspace/v1/{workspaceId}/members/search
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - page: Page number
  - size: Page size
  - sortBy: Sort field
  - sortDir: Sort direction
  - role: Role filter
  - status: Status filter
  - department: Department filter
  - search_query: Text search across name/email
```

#### Member Statistics
```http
GET /workspace/v1/{workspaceId}/members/statistics
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Bulk Operations

```http
PUT /workspace/v1/{workspaceId}/members/bulk
DELETE /workspace/v1/{workspaceId}/members/bulk
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Export Members Data

```http
GET /workspace/v1/{workspaceId}/members/export
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - format: Export format (CSV, EXCEL)
  - role: Role filter
  - status: Status filter
  - department: Department filter
```

#### Get Departments

```http
GET /workspace/v1/{workspaceId}/members/departments
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Update Member Status

```http
PATCH /workspace/v1/{workspaceId}/members/{memberId}/status
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json
{
  "status": "ACTIVE",
  "reason": "Member reactivated"
}
```

**Permission Required:** MEMBER_MANAGE

#### Get Available Roles

```http
GET /workspace/v1/{workspaceId}/roles
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Get Available Permissions

```http
GET /workspace/v1/{workspaceId}/permissions
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

### 🧩 Module Management (`/workspace/v1/modules`)

#### Get Module Overview
```http
GET /workspace/v1/modules
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "workspace_id": "WS_ABC123_XYZ789",
    "message": "Module management is available",
    "total_modules": 8,
    "active_modules": 6,
    "module_categories": [
      "CUSTOMER_MANAGEMENT",
      "SALES_MANAGEMENT",
      "INVENTORY_MANAGEMENT"
    ],
    "recent_activity": {
      "last_installed": "Product Catalog Module",
      "last_configured": "Customer CRM Module",
      "last_accessed": "2025-01-15T10:30:00Z"
    }
  }
}
```

#### Get Module Details
```http
GET /workspace/v1/modules/{moduleId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Browse Available Modules
```http
GET /workspace/v1/modules/available
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - category: Filter by module category (CUSTOMER_MANAGEMENT, SALES_MANAGEMENT, etc.)
  - featured: Show only featured modules (true/false)
```

#### Install Module

```http
POST /workspace/v1/modules/install/{moduleCode}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** ADMIN role

#### Perform Module Action

```http
POST /workspace/v1/modules/{moduleId}/action?action={action}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Available Actions:**

- **State Management**: `enable`, `disable`, `restart`, `refresh`
- **Configuration**: `configure`, `reset`, `backup`, `restore`
- **Maintenance**: `update`, `diagnose`, `optimize`, `cleanup`
- **Analytics**: `analyze`, `report`, `audit`

#### Uninstall Module

```http
DELETE /workspace/v1/modules/{moduleId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

**Permission Required:** ADMIN role

### 📊 Activity & Analytics (`/workspace/v1/{workspaceId}/activities`)

#### Get Activity Feed
```http
GET /workspace/v1/{workspaceId}/activities
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - page: Page number
  - size: Page size
  - type: Activity type filter
  - userId: Filter by specific user
  - fromDate: Start date filter
  - toDate: End date filter
```

## Role-based Access Control

### Role Hierarchy System

The Workspace module implements a 6-level hierarchical role system with granular permissions:

- **OWNER (100)** - Full workspace control including deletion and billing
- **ADMIN (80)** - Administrative access with member and settings management
- **MANAGER (60)** - Project and team management with limited administrative access
- **MEMBER (40)** - Standard access to workspace features and collaboration
- **GUEST (20)** - Limited access for external collaborators
- **VIEWER (10)** - Read-only access to workspace content

### Permission Matrix

Based on actual implementation in the codebase:

| **Permission**      | **OWNER** | **ADMIN** | **MANAGER** | **MEMBER** | **GUEST** | **VIEWER** |
|---------------------|-----------|-----------|-------------|------------|-----------|------------|
| WORKSPACE_MANAGE    | ✅         | ✅         | ❌           | ❌          | ❌         | ❌          |
| WORKSPACE_DELETE    | ✅         | ✅         | ❌           | ❌          | ❌         | ❌          |
| MEMBER_VIEW         | ✅         | ✅         | ✅           | ✅          | ✅         | ✅          |
| MEMBER_INVITE       | ✅         | ✅         | ✅           | ❌          | ❌         | ❌          |
| MEMBER_MANAGE       | ✅         | ✅         | ✅           | ❌          | ❌         | ❌          |
| MEMBER_DELETE       | ✅         | ✅         | ✅           | ❌          | ❌         | ❌          |
| VIEW_TEAMS          | ✅         | ✅         | ✅           | ✅          | 📝¹       | 📝¹        |
| MANAGE_TEAMS        | ✅         | ✅         | ✅           | ❌          | ❌         | ❌          |
| MANAGE_TEAM_MEMBERS | ✅         | ✅         | ✅           | ❌          | ❌         | ❌          |

¹ _Team viewing may be restricted based on team membership_

### Permission Definitions

- **WORKSPACE_MANAGE**: Update workspace settings, details, and configuration
- **WORKSPACE_DELETE**: Archive or permanently delete workspace
- **MEMBER_VIEW**: View workspace member directory and basic member information
- **MEMBER_INVITE**: Send invitations to new workspace members
- **MEMBER_MANAGE**: Update member roles, permissions, and settings
- **MEMBER_DELETE**: Remove members from workspace
- **VIEW_TEAMS**: View team information and member assignments
- **MANAGE_TEAMS**: Create, update, and delete teams
- **MANAGE_TEAM_MEMBERS**: Add and remove members from teams

### Role Hierarchy Inheritance

Each role inherits permissions from lower-level roles:

- **OWNER**: All permissions + workspace deletion
- **ADMIN**: All management permissions except workspace deletion
- **MANAGER**: Team management + member management permissions
- **MEMBER**: Basic viewing permissions
- **GUEST**: Limited viewing permissions
- **VIEWER**: Read-only access

### Common Headers & Response Format

#### Required Headers
```http
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}  # Required for workspace-scoped operations
Content-Type: application/json  # For POST/PUT requests
```

#### Standard API Response
```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## Security & Validation

### Multi-Tenant Security

- **Workspace Isolation**: Complete data separation between workspaces
- **Permission Caching**: Efficient role-based access control with caching
- **Session Validation**: Multi-device session management and security
- **Audit Logging**: Complete activity tracking for compliance and security

### Data Validation

- **Business Registration**: GST, PAN, and other regulatory ID validation
- **Geographic Data**: Address validation and location standardization
- **Contact Verification**: Email and phone number validation
- **Role Assignment**: Hierarchical role validation and permission checking
- **Team Constraints**: Team capacity limits and membership validation

## Dependencies

### Core Spring Dependencies

- **Spring Boot Starter Web** - REST API endpoints and web functionality
- **Spring Boot Starter Data JPA** - Database access and entity management
- **Spring Boot Starter Security** - Authentication and authorization framework
- **Spring Boot Starter Validation** - Request validation and constraint checking
- **Spring Boot Starter Cache** - Permission and data caching

### Ampairs Module Dependencies

- **Core Module** - Multi-tenancy framework, base entities, and exception handling
- **Auth Module** - User authentication, JWT validation, and session management
- **Notification Module** - Workspace event notifications and alerts

### External Libraries

- **Jackson** - JSON processing and snake_case property naming
- **Hibernate** - ORM mapping and JSON column support
- **Swagger/OpenAPI** - API documentation and endpoint specifications

## Error Handling

### Workspace-Specific Exceptions

- **`WorkspaceNotFoundException`** - Workspace does not exist or access denied
- **`WorkspaceAccessDeniedException`** - Insufficient permissions for operation
- **`TenantAccessDeniedException`** - Cross-tenant access attempt blocked
- **`InvalidWorkspaceDataException`** - Business validation failures
- **`UserWorkspaceAssociationException`** - Member relationship conflicts
- **`DuplicateWorkspaceException`** - Workspace naming conflicts

### Team-Specific Exceptions

- **`TeamNotFoundException`** - Team does not exist in workspace
- **`TeamCapacityExceededException`** - Team member limit reached
- **`InvalidTeamLeadException`** - Team lead validation failures

### Standard Error Response
```json
{
  "success": false,
  "error": {
    "code": "WORKSPACE_ACCESS_DENIED",
    "message": "Insufficient permissions to access this workspace",
    "details": {
      "required_role": "ADMIN",
      "current_role": "MEMBER"
    }
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## Testing

### Comprehensive Test Coverage

- **Unit Tests**: Service logic, validation rules, and business operations
- **Integration Tests**: End-to-end workflows, multi-tenant isolation, and API endpoints
- **Security Tests**: Role-based access control, permission validation, and tenant boundaries
- **Performance Tests**: Large workspace handling, bulk operations, and caching efficiency

### Key Test Scenarios

- **Multi-Tenant Isolation**: Ensuring complete data separation between workspaces
- **Role Hierarchy**: Testing permission inheritance and role-based access
- **Team Management**: Validating team operations and member assignments
- **Invitation Lifecycle**: Complete invitation workflow from creation to acceptance
- **Audit Compliance**: Activity logging and data traceability

## Build & Deployment

```bash
# Build workspace module
./gradlew :workspace:build

# Run comprehensive tests
./gradlew :workspace:test

# Run integration tests only
./gradlew :workspace:integrationTest

# Generate test coverage report
./gradlew :workspace:jacocoTestReport
```

## Usage Examples

### Service Integration Pattern
```kotlin
@Service
@Transactional
class BusinessLogicService(
    private val workspaceService: WorkspaceService,
    private val teamService: WorkspaceTeamService,
    private val memberService: WorkspaceMemberService
) {

    fun setupNewCompany(request: CompanySetupRequest): WorkspaceResponse {
        // Create workspace with owner
        val workspace = workspaceService.createWorkspace(request.workspaceDetails)

        // Create default teams
        val salesTeam = teamService.createTeam(
            workspace.id, CreateTeamRequest(
                name = "Sales Team",
                department = "Sales",
                permissions = setOf(WorkspacePermission.VIEW_CUSTOMERS, WorkspacePermission.MANAGE_ORDERS)
            )
        )

        return workspace
    }
}
```

### Security Integration Pattern
```kotlin
@RestController
@RequestMapping("/api/v1/crm")
@SecurityRequirement(name = "BearerAuth")
class CrmController {

    @GetMapping("/customers")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'VIEW_CUSTOMERS')")
    fun getCustomers(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @CurrentUser sessionUser: SessionUser
    ): ResponseEntity<PageResponse<CustomerResponse>> {
        // Workspace-scoped customer access with role validation
        val customers = customerService.getCustomersByWorkspace(workspaceId)
        return ResponseEntity.ok(ApiResponse.success(customers))
    }
}
```

## Module Integration

The Workspace module serves as the foundational multi-tenant layer for the entire Ampairs ecosystem:

### **🔗 Core Integration Points**

- **Core Module**: Provides multi-tenancy framework, base entities, and exception handling patterns
- **Auth Module**: Integrates JWT authentication with workspace-scoped session management
- **Notification Module**: Workspace event notifications for member activities and system updates

### **📊 Business Module Integration**

- **Customer Module**: Customer data scoped to specific workspaces with team-based access control
- **Product Module**: Product catalogs isolated per workspace with role-based management
- **Order/Invoice Modules**: Transaction processing within workspace boundaries
- **Analytics Module**: Workspace-specific reporting and business intelligence

### **🏗️ Architectural Role**

The Workspace module establishes the organizational foundation for all business operations, ensuring:

- **Complete Tenant Isolation**: All business data is properly scoped to workspace contexts
- **Hierarchical Access Control**: Fine-grained permissions cascade through all integrated modules
- **Scalable Team Organization**: Department and team-based data access across all business functions
- **Comprehensive Audit Trail**: Activity tracking extends to all workspace-scoped operations

This multi-tenant architecture enables Ampairs to serve multiple organizations simultaneously while maintaining complete
data security and access control boundaries.