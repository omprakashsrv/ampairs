# Workspace Access Control Implementation

## Overview

The `SessionUserFilter` implements workspace-based access control with proper tenant ID management for multi-tenancy
support. This ensures that users can only access resources within workspaces where they have valid membership.

## How It Works

### 1. **Request Flow**

```
HTTP Request → SessionUserFilter → Workspace Validation → Tenant Context → Repository Access
```

### 2. **Access Control Process**

1. **Authentication Check**: Verifies user is authenticated
2. **Workspace Header Check**: Requires `X-Workspace` header with workspace ID
3. **Membership Validation**: Confirms user is a member of the requested workspace
4. **Tenant Context Setting**: Sets the workspace ID as tenant context for repository queries
5. **Request Processing**: Continues with the request
6. **Cleanup**: Clears tenant context after request completion

### 3. **Tenant ID Repository Integration**

The tenant ID is automatically applied to repository queries through:

- **OwnableBaseDomain**: All entities extending this base class automatically get tenant filtering
- **@TenantId Annotation**: The `ownerId` field is marked as tenant identifier
- **TenantContext**: Thread-local storage maintains current tenant (workspace ID)
- **Hibernate Integration**: Automatic WHERE clauses filter data by tenant

## Usage

### For Client Applications

#### 1. **Required Headers**

```http
Authorization: Bearer <jwt_token>
X-Workspace: <workspace_id>
```

#### 2. **Example API Call**

```javascript
// JavaScript/TypeScript example
const response = await fetch('/workspace/v1/members', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'X-Workspace': 'workspace-uuid-here',
    'Content-Type': 'application/json'
  }
});
```

#### 3. **Mobile App Usage (Android/iOS)**

```kotlin
// Kotlin example for mobile apps
val request = Request.Builder()
    .url("${BASE_URL}/workspace/v1/members")
    .header("Authorization", "Bearer $accessToken")
    .header("X-Workspace", workspaceId)
    .build()
```

### For Backend Development

#### 1. **Repository Queries**

All repository queries automatically filter by tenant (workspace):

```kotlin
// This query automatically adds: WHERE owner_id = :currentTenantId
val members = memberRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId, pageable)
```

#### 2. **Service Layer Access**

```kotlin
@Service
class MyService {
    fun getWorkspaceData(workspaceId: String): List<MyEntity> {
        // TenantContext.getCurrentTenant() returns the current workspace ID
        val currentTenant = TenantContext.getCurrentTenant()
        
        // Repository queries are automatically filtered by tenant
        return myRepository.findAll()
    }
}
```

#### 3. **Manual Tenant Operations**

```kotlin
// Execute code with specific tenant context
TenantContext.withTenant("workspace-id") {
    // All repository operations here use the specified workspace
    val data = repository.findAll()
    return data
}

// Get current tenant
val currentWorkspace = TenantContext.getCurrentTenant()

// Require tenant (throws exception if not set)
val workspaceId = TenantContext.requireCurrentTenant()
```

## Security Features

### 1. **Automatic Tenant Isolation**

- All database queries are automatically scoped to the current workspace
- Prevents cross-workspace data leakage
- No need for manual WHERE clauses in repositories

### 2. **Membership Validation**

- Validates user membership before setting tenant context
- Checks active membership status
- Logs access attempts for security auditing

### 3. **Request Path Filtering**

The filter skips certain endpoints that don't require workspace context:

- `/auth/v1/*` - Authentication endpoints
- `/user/v1/*` - User profile endpoints
- `/workspace/v1/check-slug` - Public workspace slug validation
- `/actuator/health` - Health checks
- `/swagger*` - API documentation
- `/api-docs` - OpenAPI documentation

## Error Responses

### 1. **Missing Authentication**

```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Workspace access denied",
    "details": "Authentication required"
  }
}
```

### 2. **Missing Workspace Header**

```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED", 
    "message": "Workspace access denied",
    "details": "Workspace header (X-Workspace) is required"
  }
}
```

### 3. **Invalid Workspace Access**

```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Workspace access denied", 
    "details": "You don't have access to this workspace"
  }
}
```

## Configuration

### 1. **Filter Order**

The filter is automatically registered as a Spring component and runs after authentication filters.

### 2. **Logging Configuration**

```yaml
logging:
  level:
    com.ampairs.workspace.filter.SessionUserFilter: DEBUG
```

### 3. **Database Configuration**

Ensure your entities extend `OwnableBaseDomain` for automatic tenant filtering:

```kotlin
@Entity
@Table(name = "my_entities")
class MyEntity : OwnableBaseDomain() {
    // Entity fields...
}
```

## Best Practices

### 1. **Client-Side**

- Always include `X-Workspace` header in API calls
- Store current workspace context in application state
- Handle workspace access errors gracefully

### 2. **Backend Development**

- Extend `OwnableBaseDomain` for new entities that need tenant isolation
- Use service methods rather than direct repository access for business logic
- Validate workspace context in service methods when needed

### 3. **Testing**

- Mock `TenantContext.setCurrentTenant()` in unit tests
- Use `@WithMockUser` and set tenant context in integration tests
- Test cross-workspace access scenarios to ensure proper isolation

## Troubleshooting

### Common Issues

1. **Cross-workspace data access**: Ensure entities extend `OwnableBaseDomain`
2. **Missing tenant context**: Check that `X-Workspace` header is included
3. **Repository queries not filtered**: Verify entity inheritance and database schema
4. **Filter not applied**: Check request path against `shouldSkipFilter()` logic

### Debug Steps

1. Enable debug logging for the filter
2. Check tenant context in service methods: `TenantContext.getCurrentTenant()`
3. Verify user membership: `memberService.isWorkspaceMember(workspaceId, userId)`
4. Inspect database queries for automatic WHERE clauses