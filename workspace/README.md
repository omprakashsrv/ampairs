# Workspace Module

## Overview

The Workspace module provides company/workspace management with role-based access control. It handles multi-tenant
company operations, user-company associations, role management, and session-based user context within the Ampairs
application.

## Architecture

### Package Structure

```
com.ampairs.workspace/
├── config/                # Configuration constants
├── controller/            # REST API endpoints
├── exception/             # Workspace exception handling
├── filter/                # Request filters
├── model/                 # Workspace entities and DTOs
│   ├── dto/               # Data Transfer Objects
│   └── enums/             # Workspace enumerations
├── repository/            # Data access layer
└── service/               # Workspace business logic
```

## Key Components

### Controllers

- **`WorkspaceController.kt`** - Main workspace management endpoints for CRUD operations

### Models

#### Core Entities

- **`Workspace.kt`** - Company/workspace entity with comprehensive business information

#### DTOs

- **`WorkspaceRequest.kt`** - Workspace creation and update request
- **`WorkspaceResponse.kt`** - Workspace information response

#### Enumerations

- **`Role.kt`** - User role definitions within workspaces

### Services

- **`WorkspaceService.kt`** - Core business logic for workspace management, user associations, and role handling

### Repositories

- **`WorkspaceRepository.kt`** - Workspace data access operations
- **`UserCompanyRepository.kt`** - User-company relationship management

### Filters

- **`SessionUserFilter.kt`** - HTTP filter for session user context management

### Configuration

- **`Constants.kt`** - Workspace-specific constants and configuration values

## Key Features

### Multi-tenant Workspace Management

- Complete company profile management
- Business registration details (GST, PAN, etc.)
- Geographic location and address handling
- Contact information management
- Business type and category classification

### Role-based Access Control

- User-workspace associations with roles
- Permission-based access control
- Role hierarchy management
- Multi-workspace user support

### Session Management

- Session-based user context
- Workspace-scoped permissions
- Request-level user identification
- Multi-tenant session isolation

### Geographic Support

- Location-based workspace organization
- Address standardization
- Geographic search and filtering
- Location-based access control

## Data Model

### Workspace Entity Structure

```kotlin
data class Workspace(
    val name: String,
    val description: String?,
    val businessType: String?,
    val gstNumber: String?,
    val panNumber: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val website: String?,
    val address: Address?,
    val location: Location?
) : OwnableBaseDomain()
```

### Session User Context

```kotlin
data class SessionUser(
    val userId: String,
    val workspaceId: String,
    val role: Role,
    val permissions: Set<String>
)
```

## API Endpoints

### Workspace Management

```http
GET /workspace/v1/workspaces
Authorization: Bearer <access-token>
```

```http
POST /workspace/v1/workspaces
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "ACME Corporation",
  "description": "Software development company",
  "businessType": "PRIVATE_LIMITED",
  "gstNumber": "29AABCU9603R1ZX",
  "panNumber": "AABCU9603R",
#### Create Workspace
    "city": "Bangalore",
    "state": "Karnataka",
    "country": "India",
    "pincode": "560001"

  }
}
```

  "subscriptionPlan": "ENTERPRISE",
  "workspaceType": "BUSINESS",
  "contactDetails": {
    "email": "info@acme.com",
    "phone": "+91-9876543210",
    "website": "https://acme.com"
  },
  "registration": {
    "gstNumber": "29AABCU9603R1ZX",
    "panNumber": "AABCU9603R"
  },
```http
PUT /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "ACME Corp Updated",
  "description": "Updated description"
}
```

#### List & Search Workspaces
```http
GET /workspace/v1/workspaces
Authorization: Bearer <access-token>
Query Parameters:
  - page: Page number (default: 0)
  - size: Page size (default: 20)
  - sort: Sort field (default: createdAt,desc)
  - query: Search text
  - type: Workspace type filter
  - status: Status filter
```

#### Get Workspace Details
```http
DELETE /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Update Workspace
### User-Workspace Management

```http
X-Workspace-ID: {workspaceId}
GET /workspace/v1/workspaces/{workspaceId}/users

Authorization: Bearer <access-token>
  "name": "Updated Name",
  "description": "Updated description",
  "contactDetails": {
    "email": "updated@acme.com"
  }
```http
POST /workspace/v1/workspaces/{workspaceId}/users
Authorization: Bearer <access-token>
### Member Management

#### List Members
Content-Type: application/json
GET /workspace/v1/workspaces/{workspaceId}/members
  "userId": "user-uuid",
X-Workspace-ID: {workspaceId}
Query Parameters:
  - page: Page number
  - size: Page size
  - sort: Sort field
  - query: Search text
  - role: Role filter
  - status: Status filter
  "role": "ADMIN"
}
#### Add Member
```http
POST /workspace/v1/workspaces/{workspaceId}/members
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json

{
  "userId": "user-uuid",
  "role": "ADMIN",
  "permissions": ["MANAGE_MEMBERS", "VIEW_REPORTS"]
}
Content-Type: application/json
{
#### Update Member
  "role": "MANAGER"
PUT /workspace/v1/workspaces/{workspaceId}/members/{memberId}
Authorization: Bearer <access-token}
X-Workspace-ID: {workspaceId}
Content-Type: application/json

{
  "role": "MANAGER",
  "isActive": true,
  "permissions": ["VIEW_REPORTS"]
}
```

#### Remove Member
```http
DELETE /workspace/v1/workspaces/{workspaceId}/members/{memberId}
```
X-Workspace-ID: {workspaceId}
```

### Module Management

#### List Available Modules
```http
GET /workspace/v1/workspaces/{workspaceId}/modules
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
```

#### Enable Module
```http
POST /workspace/v1/workspaces/{workspaceId}/modules
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}


## Role-based Access Control
  "moduleId": "module-uuid",
  "settings": {
    "enabled": true,
    "configurations": {}
  }

```kotlin
enum class Role {
### Invitation Management

#### Create Invitation
    OWNER,      // Full workspace control
POST /workspace/v1/workspaces/{workspaceId}/invitations
    MANAGER,    // Management access
X-Workspace-ID: {workspaceId}
    EMPLOYEE,   // Standard employee access

    VIEWER      // Read-only access
  "email": "user@example.com",
  "role": "MEMBER",
  "expiresIn": "7d",
  "message": "Join our workspace!"
```

### Permission Matrix
#### List Invitations
```http
GET /workspace/v1/workspaces/{workspaceId}/invitations
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Query Parameters:
  - status: Invitation status filter
  - page: Page number
  - size: Page size
```
| Operation         | OWNER | ADMIN | MANAGER | EMPLOYEE | VIEWER  |
### Common Headers
| Workspace CRUD    | ✓     | ✓     | ✗       | ✗        | ✗       |
All workspace-scoped endpoints require:
```http
Authorization: Bearer <access-token>
X-Workspace-ID: {workspaceId}
Content-Type: application/json  # For POST/PUT requests

```yaml
### Response Format
  workspace:
Success Response:
```json
{
  "success": true,
  "data": {
    // Response data
  },
  "message": "Operation completed successfully"
}
```

Error Response:
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": {
      "field": "Additional error details if any"
    }
  }
}
```
- Permission caching
- Session validation

### Data Validation

- Business registration validation (GST, PAN)
- Geographic data validation
- Contact information verification
- Role assignment validation

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation

### Integration Dependencies

- Core Module (Multi-tenancy, Base entities)
- Auth Module (User authentication)
- Jackson (JSON processing)

## Error Handling

### Workspace Errors

- Workspace not found
- Insufficient permissions
- Invalid business registration data
- Duplicate workspace names
- User already associated with workspace

### Response Format

```json
{
  "success": false,
  "error": {
    "code": "WORKSPACE_NOT_FOUND",
    "message": "The specified workspace does not exist or you don't have access",
    "timestamp": "2023-01-01T12:00:00Z"
  }
}
```

## Testing

### Unit Tests

- Workspace CRUD operations
- Role-based access control
- User-workspace associations
- Permission validation
- Session management

### Integration Tests

- End-to-end workspace workflows
- Multi-tenant data isolation
- Role transition scenarios
- Security boundary testing

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :workspace:build

# Run tests
./gradlew :workspace:test

# Run specific test
./gradlew :workspace:test --tests WorkspaceServiceTest
```

## Usage Examples

### Service Integration

```kotlin
@Service
class BusinessService(
    private val workspaceService: WorkspaceService
) {

    fun createCompanyWorkspace(request: WorkspaceRequest): WorkspaceResponse {
        return workspaceService.createWorkspace(request)
    }

    fun addUserToWorkspace(workspaceId: String, userId: String, role: Role) {
        workspaceService.addUserToWorkspace(workspaceId, userId, role)
    }
}
```

### Controller Security

```kotlin
@RestController
@RequestMapping("/api/v1/business")
class BusinessController {

    @GetMapping("/data")
    @PreAuthorize("hasRole('EMPLOYEE')")
    fun getBusinessData(@CurrentUser sessionUser: SessionUser): ResponseEntity<*> {
        val workspaceId = sessionUser.workspaceId
        // Access workspace-scoped data
    }
}
```


## Integration

This module integrates with:

- **Core Module**: Multi-tenancy support, base entities, exception handling
- **Auth Module**: User authentication and JWT token validation
- **Customer Module**: Customer-workspace associations
- **Product Module**: Workspace-scoped product catalog
- **Order/Invoice Modules**: Workspace-based transaction processing

The Workspace module provides the organizational foundation for all business operations within the Ampairs application,
ensuring proper data isolation and access control across different workspaces and user roles.