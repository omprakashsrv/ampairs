# Research: Business Module Implementation

**Feature**: 003-business-module
**Date**: 2025-10-10
**Status**: Complete

## Research Questions

### 1. Module Structure and Dependencies

**Decision**: Follow existing module pattern (customer, product, order, invoice)

**Rationale**:
- Ampairs uses a modular monolith architecture with separate Gradle modules
- Each business domain has its own module under `ampairs-backend/`
- Modules depend on `core` for shared utilities and base classes
- Clear separation of concerns with consistent package structure

**Alternatives Considered**:
- Monolithic structure: Rejected - doesn't match existing architecture
- Microservice: Rejected - overkill for this scope, would break existing patterns

**Implementation Pattern**:
```kotlin
ampairs-backend/business/
├── build.gradle.kts (dependencies: core, workspace for context)
└── src/main/kotlin/com/ampairs/business/
    ├── model/         # Entities
    ├── model/dto/     # Request/Response DTOs
    ├── repository/    # Spring Data JPA
    ├── service/       # Business logic
    ├── controller/    # REST endpoints
    └── exception/     # Error handling
```

### 2. Entity Design and Relationships

**Decision**: One-to-one relationship between Workspace and Business

**Rationale**:
- Current requirement: 1 workspace = 1 business profile
- Simplest approach for initial implementation
- Easier migration from existing Workspace fields
- Foreign key: `business.workspace_id` → `workspaces.uid` (unique constraint)

**Alternatives Considered**:
- One-to-many (workspace → businesses): Future consideration for multi-location support
- Many-to-many: Rejected - unnecessary complexity
- Embedded entity: Rejected - doesn't solve the bloat problem

**Entity Fields** (based on Workspace analysis):
```kotlin
@Entity
@Table(name = "businesses")
class Business : OwnableBaseDomain() {
    // Profile
    var name: String
    var businessType: BusinessType  // ENUM
    var description: String?
    var ownerName: String?

    // Address (structured fields)
    var addressLine1: String?
    var addressLine2: String?
    var city: String?
    var state: String?
    var postalCode: String?
    var country: String?

    // Location (for future features)
    var latitude: Double?
    var longitude: Double?

    // Contact
    var phone: String?
    var email: String?
    var website: String?

    // Tax/Regulatory
    var taxId: String?
    var registrationNumber: String?

    // Operating Config
    var timezone: String
    var currency: String
    var language: String

    // Business Hours
    var openingHours: String?
    var closingHours: String?
    var operatingDays: String  // JSON array

    // Formatting
    var dateFormat: String
    var timeFormat: String

    // Relationships
    @TenantId
    var workspaceId: String  // FK to workspace, tenant filter
}
```

### 3. Multi-Tenancy with @TenantId

**Decision**: Use @TenantId annotation on workspaceId field

**Rationale**:
- Existing pattern used across all tenant-aware entities
- Automatic filtering by Hibernate multi-tenancy support
- Prevents cross-tenant data leakage
- workspaceId acts as both FK and tenant discriminator

**Implementation**:
```kotlin
@Entity
class Business : OwnableBaseDomain() {
    @TenantId
    @Column(name = "workspace_id", nullable = false, unique = true)
    var workspaceId: String = ""
    // ...
}
```

**Controller Pattern**:
```kotlin
@RestController
@RequestMapping("/api/v1/business")
class BusinessController {
    @GetMapping
    fun getBusiness(): ApiResponse<BusinessResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
        val business = service.findByWorkspaceId(workspaceId)
        return ApiResponse.success(business.asBusinessResponse())
    }
}
```

### 4. DTO Pattern for API Contracts

**Decision**: Separate Request/Response DTOs, never expose entities

**Rationale**:
- Project guideline: NEVER expose JPA entities in controllers
- Security: Only expose fields clients need
- Flexibility: API changes don't require entity changes
- Validation: Request DTOs have Jakarta validation annotations

**DTO Structure**:
```kotlin
// Response DTO
data class BusinessResponse(
    val uid: String,
    val name: String,
    val businessType: String,
    val addressLine1: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val phone: String?,
    val email: String?,
    val timezone: String,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Request DTOs
data class BusinessCreateRequest(
    @field:NotBlank(message = "Business name is required")
    val name: String,

    @field:NotNull
    val businessType: BusinessType,

    @field:Email(message = "Invalid email format")
    val email: String?,

    // ... other fields with validation
)

data class BusinessUpdateRequest(
    val name: String?,
    val addressLine1: String?,
    // ... optional fields for partial update
)

// Extension functions for mapping
fun Business.asBusinessResponse(): BusinessResponse = BusinessResponse(...)
fun BusinessCreateRequest.toBusiness(workspaceId: String): Business = Business().apply { ... }
```

### 5. Data Migration Strategy

**Decision**: Two-phase Flyway migration with backward compatibility

**Rationale**:
- Minimize downtime and risk
- Allow gradual transition
- Maintain data integrity
- Enable rollback if needed

**Migration Phases**:

**Phase 1: V1.0.0__create_businesses_table.sql**
```sql
CREATE TABLE businesses (
    uid VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    address_line1 VARCHAR(255),
    -- ... all fields
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(uid) ON DELETE CASCADE
);

CREATE INDEX idx_business_workspace ON businesses(workspace_id);
CREATE INDEX idx_business_type ON businesses(business_type);
```

**Phase 2: V1.0.1__migrate_workspace_business_data.sql**
```sql
INSERT INTO businesses (
    uid, workspace_id, name, business_type,
    address_line1, address_line2, city, state, postal_code, country,
    phone, email, website, tax_id, registration_number,
    timezone, currency, language, date_format, time_format,
    created_at, updated_at
)
SELECT
    gen_random_uuid()::VARCHAR,  -- Generate new UID
    uid AS workspace_id,
    name,
    workspace_type AS business_type,
    address_line1, address_line2, city, state, postal_code, country,
    phone, email, website, tax_id, registration_number,
    timezone, currency, language, date_format, time_format,
    created_at, updated_at
FROM workspaces
WHERE active = true;  -- Only migrate active workspaces
```

**Future Phase (after frontend migration)**: Remove deprecated workspace fields

### 6. API Endpoint Design

**Decision**: RESTful API at /api/v1/business (singular resource)

**Rationale**:
- One business per workspace (1:1 relationship)
- Singular noun for singleton resource (REST best practice)
- Workspace context implicit from X-Workspace-ID header
- No need for business ID in URL (derived from workspace context)

**Endpoints**:
```
GET    /api/v1/business           → Get current workspace's business profile
POST   /api/v1/business           → Create business profile (workspace setup)
PUT    /api/v1/business           → Update business profile (full update)
PATCH  /api/v1/business           → Partial update (optional, future)
```

**Response Format**:
```json
{
  "success": true,
  "data": {
    "uid": "bus_abc123",
    "name": "Acme Corp",
    "businessType": "RETAIL",
    "addressLine1": "123 Main St",
    "city": "Mumbai",
    "state": "Maharashtra",
    "country": "India",
    "phone": "+91-22-12345678",
    "email": "info@acme.com",
    "timezone": "Asia/Kolkata",
    "currency": "INR",
    "createdAt": "2025-10-10T10:00:00Z",
    "updatedAt": "2025-10-10T10:00:00Z"
  },
  "timestamp": "2025-10-10T10:00:00Z",
  "path": "/api/v1/business"
}
```

### 7. Validation Rules

**Decision**: Jakarta validation annotations + service-level business rules

**Rationale**:
- Field-level validation: Jakarta annotations on DTOs
- Business rules: Service layer validation
- Consistent with existing modules

**Validation Examples**:
```kotlin
data class BusinessCreateRequest(
    @field:NotBlank(message = "Business name is required")
    @field:Size(min = 2, max = 255, message = "Name must be 2-255 characters")
    val name: String,

    @field:NotNull(message = "Business type is required")
    val businessType: BusinessType,

    @field:Email(message = "Invalid email format")
    val email: String?,

    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    val phone: String?,

    @field:Size(max = 500, message = "Website URL too long")
    val website: String?,

    @field:Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
    val currency: String = "INR"
)
```

### 8. Testing Strategy

**Decision**: Integration tests with Testcontainers + Unit tests for business logic

**Rationale**:
- Constitution requirement: Real dependencies (actual DB)
- Integration tests verify full stack (controller → service → repository → DB)
- Unit tests for service business logic only
- Contract tests verify API schema

**Test Structure**:
```kotlin
@SpringBootTest
@Testcontainers
class BusinessIntegrationTest {
    @Container
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")

    @Test
    fun `should create business profile`() {
        // Given: Valid business data
        // When: POST /api/v1/business
        // Then: 201 Created, business in DB
    }

    @Test
    fun `should retrieve business by workspace ID`() {
        // Given: Business exists
        // When: GET /api/v1/business with X-Workspace-ID
        // Then: 200 OK with business data
    }

    @Test
    fun `should not leak cross-tenant data`() {
        // Given: 2 workspaces with businesses
        // When: Request with workspace A's context
        // Then: Only workspace A's business returned
    }
}
```

### 9. Exception Handling

**Decision**: Global exception handler with ApiResponse error format

**Rationale**:
- Existing pattern: BusinessExceptionHandler extends global handler
- Consistent error responses across all modules
- HTTP status codes mapped to exception types

**Exception Types**:
```kotlin
class BusinessNotFoundException(workspaceId: String) :
    ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found for workspace: $workspaceId")

class BusinessAlreadyExistsException(workspaceId: String) :
    ResponseStatusException(HttpStatus.CONFLICT, "Business already exists for workspace: $workspaceId")

class InvalidBusinessDataException(message: String) :
    ResponseStatusException(HttpStatus.BAD_REQUEST, message)
```

**Error Response**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BUSINESS_NOT_FOUND",
    "message": "Business not found for workspace: wsp_123",
    "details": null
  },
  "timestamp": "2025-10-10T10:00:00Z",
  "path": "/api/v1/business",
  "traceId": "abc-def-123"
}
```

### 10. Performance Considerations

**Decision**: Database indexing + @EntityGraph for relationships

**Rationale**:
- workspace_id is most common query filter → unique index
- business_type for filtering/analytics → regular index
- Future: If workspace → business relationship needed, use @EntityGraph

**Indexes**:
```sql
CREATE UNIQUE INDEX idx_business_workspace ON businesses(workspace_id);
CREATE INDEX idx_business_type ON businesses(business_type);
CREATE INDEX idx_business_active ON businesses(active) WHERE active = true;
```

**Query Pattern**:
```kotlin
interface BusinessRepository : JpaRepository<Business, String> {
    fun findByWorkspaceId(workspaceId: String): Business?

    @Query("SELECT b FROM Business b WHERE b.workspaceId = :workspaceId AND b.active = true")
    fun findActiveByWorkspaceId(workspaceId: String): Business?
}
```

## Summary

All technical decisions resolved:
- ✅ Module structure: Follow existing pattern (customer/product/order)
- ✅ Entity design: One-to-one with Workspace, @TenantId on workspaceId
- ✅ DTOs: Separate Request/Response, extension functions for mapping
- ✅ API design: RESTful singular resource at /api/v1/business
- ✅ Migration: Two-phase Flyway with data copy, backward compatible
- ✅ Validation: Jakarta annotations + service-level rules
- ✅ Testing: Integration with Testcontainers + unit tests
- ✅ Multi-tenancy: @TenantId automatic filtering
- ✅ Error handling: Global handler with ApiResponse format
- ✅ Performance: Proper indexing, efficient queries

**No NEEDS CLARIFICATION remaining. Ready for Phase 1: Design & Contracts.**
