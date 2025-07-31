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
- **`UserCompany.kt`** - User-company association with role-based access control
- **`SessionUser.kt`** - Session context for user permissions and workspace access
- **`Location.kt`** - Geographic location and address information

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

### Multi-tenant Company Management

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

### User-Company Association

```kotlin
data class UserCompany(
    val userId: String,
    val workspaceId: String,
    val role: Role,
    val isActive: Boolean,
    val joinedAt: LocalDateTime
) : BaseDomain()
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
  "contactEmail": "info@acme.com",
  "contactPhone": "+91-9876543210",
  "website": "https://acme.com",
  "address": {
    "street": "123 Business Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "country": "India",
    "pincode": "560001"
  }
}
```

```http
GET /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
```

```http
PUT /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "ACME Corp Updated",
  "description": "Updated description"
}
```

```http
DELETE /workspace/v1/workspaces/{workspaceId}
Authorization: Bearer <access-token>
```

### User-Workspace Management

```http
GET /workspace/v1/workspaces/{workspaceId}/users
Authorization: Bearer <access-token>
```

```http
POST /workspace/v1/workspaces/{workspaceId}/users
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "userId": "user-uuid",
  "role": "ADMIN"
}
```

```http
PUT /workspace/v1/workspaces/{workspaceId}/users/{userId}/role
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "role": "MANAGER"
}
```

## Role-based Access Control

### Role Hierarchy

```kotlin
enum class Role {
    OWNER,      // Full workspace control
    ADMIN,      // Administrative access
    MANAGER,    // Management access
    EMPLOYEE,   // Standard employee access
    VIEWER      // Read-only access
}
```

### Permission Matrix

| Operation         | OWNER | ADMIN | MANAGER | EMPLOYEE | VIEWER  |
|-------------------|-------|-------|---------|----------|---------|
| Workspace CRUD    | ✓     | ✓     | ✗       | ✗        | ✗       |
| User Management   | ✓     | ✓     | Limited | ✗        | ✗       |
| Data Access       | ✓     | ✓     | ✓       | ✓        | ✓       |
| Data Modification | ✓     | ✓     | ✓       | ✓        | ✗       |
| Reports Access    | ✓     | ✓     | ✓       | Limited  | Limited |

## Configuration

### Required Properties

```yaml
ampairs:
  workspace:
    default-role: EMPLOYEE
    max-users-per-workspace: 100
    auto-approve-invitations: false
  multitenancy:
    enabled: true
    tenant-resolver: workspace-based
```

## Security Implementation

### Multi-tenancy Isolation

- Workspace-scoped data access
- Tenant context from JWT claims
- Row-level security implementation
- Cross-workspace data protection

### Session Management

- Request-scoped user context
- Role-based method security
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

### Session Filter Usage

```kotlin
@Component
class SessionUserFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        val sessionUser = buildSessionUser(token)

        SessionUserContext.setCurrentUser(sessionUser)
        try {
            filterChain.doFilter(request, response)
        } finally {
            SessionUserContext.clear()
        }
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
ensuring proper data isolation and access control across different companies and user roles.