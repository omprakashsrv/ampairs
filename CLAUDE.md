# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ampairs is a comprehensive business management system with **three integrated applications**:

### Flyway Migration Baseline
- Backend schema is version-controlled via Flyway migrations stored in each domain module under `src/main/resources/db/migration/mysql/` (e.g., `ampairs-backend/customer/src/main/resources/db/migration/mysql/V1.0.0__create_customer_module_tables.sql`).
- Aggregated documentation lives in `ampairs-backend/ampairs_service/src/main/resources/db/migration/` (`README.md`, `MIGRATION_BASELINE.md`).
- Integration tests for migrations live in `ampairs-backend/ampairs_service/src/test/kotlin/com/ampairs/FlywayMigrationTest.kt` (requires Docker/Testcontainers).

### **System Architecture**

1. **Backend (Spring Boot + Kotlin)** - `/ampairs_service` + domain modules
2. **Web Frontend (Angular + Material Design 3)** - `/ampairs-web` 
3. **Mobile App (Kotlin Multiplatform)** - `/ampairs-mp-app` (Android, iOS, Desktop)

**Integration**: All clients consume REST APIs from Spring Boot backend with JWT authentication and multi-tenant support.

## Development Guidelines

### **Code Organization**
- Follow existing package structure: `com.ampairs.{module}.{layer}`
- Use Kotlin data classes for DTOs
- Extend `BaseDomain` or `OwnableBaseDomain` for entities
- Maintain separation of concerns between layers

### **JSON Naming Convention**

**IMPORTANT: Global snake_case configuration handles property naming automatically**

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

**✅ CORRECT DTO Pattern (No annotations needed):**
```kotlin
data class AuthInitRequest(
    var countryCode: Int = 91,        // Maps to "country_code" 
    var recaptchaToken: String? = null, // Maps to "recaptcha_token"
    var phone: String = ""            // Maps to "phone"
)
```

**❌ INCORRECT (Redundant annotations):**
```kotlin
data class AuthInitRequest(
    @JsonProperty("country_code")     // ❌ UNNECESSARY
    var countryCode: Int = 91,
    // ...
)
```

**Key Rules:**
1. **Never add @JsonProperty for standard camelCase to snake_case conversions**
2. **Let the global Jackson configuration handle naming automatically**
3. **Only use @JsonProperty for special cases that don't follow the standard pattern**

### **Date/Time Handling (CRITICAL)**

**ALWAYS use `java.time.Instant` for timestamps - NEVER use `LocalDateTime`**

| Type | Timezone Info | Use Case | Status |
|------|---------------|----------|--------|
| `Instant` | UTC (implicit) | Timestamps, historical events, API responses | ✅ **USE THIS** |
| `LocalDateTime` | None (ambiguous) | ❌ **DEPRECATED** - causes timezone bugs | ❌ DO NOT USE |

**Why `Instant`?**
- Represents a specific point in time on the UTC timeline
- No ambiguity - always means the same moment globally
- Serializes as ISO-8601 with Z suffix: `"2025-01-09T14:30:00Z"`
- Prevents timezone-related bugs during DST transitions
- Industry standard (AWS, Google, GitHub, Stripe)

**Correct Entity Pattern:**
```kotlin
@Entity
class MyEntity : BaseDomain() {
    // ✅ CORRECT
    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()

    // ❌ WRONG - DO NOT USE
    // var createdAt: LocalDateTime = LocalDateTime.now()
}
```

**Correct DTO Pattern:**
```kotlin
data class EntityResponse(
    val uid: String,
    val name: String,
    val createdAt: Instant,    // ✅ Serializes to "2025-01-09T14:30:00Z"
    val updatedAt: Instant     // ✅ UTC timezone explicit
)
```

**Database:**
- Use `TIMESTAMP` columns (not `DATETIME`)
- Connection string must include: `?serverTimezone=UTC`
- Hibernate maps `Instant` to `TIMESTAMP` automatically

**Frontend (Angular/TypeScript):**
```typescript
// Parse API response (already in UTC)
const date = new Date(response.created_at); // "2025-01-09T14:30:00Z"

// Display in user's local timezone
{{ date | date:'medium' }} // Automatic browser timezone conversion
```

**Migration Note:**
- Legacy code may use `LocalDateTime` - this is being migrated to `Instant`
- For new features, ALWAYS use `Instant`
- Reference: `/specs/002-timezone-support/research.md`

### **Angular Web Application Design System**

**CRITICAL: Use Angular Material 3 (M3) Design System Exclusively**

- **Components**: ONLY Angular Material 3 components (`@angular/material`)
- **Prohibited**: Bootstrap, Tailwind CSS, PrimeNG, Ant Design, custom CSS frameworks
- **Theme**: Material Design 3 color system with light/dark mode support
- **Icons**: Material Design Icons only

### **Database & API Design**
- REST endpoints: `/api/v1/{resource}` pattern
- Use proper HTTP status codes and pagination for lists
- Database naming: CamelCase to underscore strategy
- All responses use `ApiResponse<T>` wrapper for consistency

### **Multi-Device Authentication**
- JWT tokens include device_id for session management
- Support multiple concurrent logins per user
- Device-specific refresh tokens and logout capabilities

### **Architectural Patterns**
- **Multi-tenancy**: Tenant-aware data isolation via `TenantContext`
- **Domain-Driven Design**: Each module represents bounded context
- **Entity Relationships**: Use `@EntityGraph` for efficient data loading
- **Error Handling**: Standardized error responses with proper HTTP codes

### **Spring Data JPA Best Practices**

#### **@EntityGraph Pattern (Preferred over JOIN FETCH)**
**Entity Level**: Define named entity graphs
```kotlin
@NamedEntityGraph(
    name = "EntityName.withRelations",
    attributeNodes = [NamedAttributeNode("relationshipProperty")]
)
class EntityName
```

**Repository Level**: Use @EntityGraph with EXISTS subquery
```kotlin
@EntityGraph("EntityName.withRelations")
@Query("SELECT e FROM Entity e WHERE ... AND EXISTS (SELECT r FROM e.relations r WHERE ...)")
fun findWithRelations(): EntityName?
```

**Benefits**: Prevents N+1 queries, cleaner separation of concerns, reusable across methods

#### **Repository Method Patterns**
- **Prefer**: Spring Data JPA derived query methods (`findByActiveTrueOrderByName()`)
- **When needed**: Custom `@Query` for complex business logic only
- **Avoid**: `JOIN FETCH` in JPQL when `@EntityGraph` achieves same result

#### **Controller Exception Handling**
- **Never**: Use try-catch blocks in controllers for business logic exceptions
- **Let exceptions bubble up**: Global exception handler converts exceptions to HTTP responses
- **Focus**: Controllers should handle HTTP concerns only, not exception mapping
- **Consistency**: Centralized error handling ensures uniform response format

#### **API Response Standardization**
- **Always**: Use `ApiResponse<T>` wrapper for all controller return types
- **Import**: `com.ampairs.core.domain.dto.ApiResponse`
- **Success**: `return ApiResponse.success(data)`
- **Consistent format**: All endpoints return `{"success": true, "data": T, "timestamp": "..."}`
- **Global error handling**: Exception handler returns `ApiResponse` with error details

#### **DTO Pattern for Controllers**
- **NEVER expose JPA entities directly** in API responses - creates security risks and tight coupling
- **Always use Response DTOs** for API outputs - only expose fields clients need
- **Always use Request DTOs** for API inputs - proper validation and input sanitization
- **Pattern**: Entity → Response DTO via extension functions (`entity.asEntityResponse()`)

**Required DTO Structure:**
```kotlin
// Response DTO - in domain/dto/ package
data class EntityResponse(
    val uid: String,
    val name: String,
    // ... only API-relevant fields, no internal fields
)

// Request DTOs - in domain/dto/ package
data class EntityCreateRequest(
    @field:NotBlank val name: String,
    // ... validation annotations
)

// Extension functions - in same DTO file
fun Entity.asEntityResponse(): EntityResponse = EntityResponse(/*...*/)
fun EntityCreateRequest.toEntity(): Entity = Entity().apply {/*...*/}
```

**Controller Implementation:**
```kotlin
@GetMapping
fun getEntities(): ApiResponse<List<EntityResponse>> {
    return ApiResponse.success(service.findAll().asEntityResponses())
}

@PostMapping
fun create(@Valid request: EntityCreateRequest): ApiResponse<EntityResponse> {
    val created = service.create(request.toEntity())
    return ApiResponse.success(created.asEntityResponse())
}
```

### **Multi-Tenant Architecture Patterns**

#### **@TenantId Best Practices**
- **Entity Field**: Add `@TenantId` to tenant identifier field (e.g., `workspaceId`)
- **Automatic Filtering**: Repository methods auto-filtered by current tenant context
- **Field Population**: Entity field auto-populated from `TenantContextHolder.getCurrentTenant()`

#### **Tenant Context Timing**
- **CRITICAL**: Set tenant context at controller level, before repository injection
- **Pattern**: Controller level tenant switching with try-finally cleanup
- **Validation**: @TenantId validation occurs at entity persist/save time
- **Service Layer**: Should NOT handle tenant context switching

#### **Cross-Tenant Data Access**
- **Native SQL**: Use `nativeQuery = true` to bypass @TenantId automatic filtering
- **JPA Queries**: Automatically filtered by @TenantId, cannot access cross-tenant data
- **Use Cases**: User workspace listing, admin operations, invitation acceptance

## Module Structure

**Foundation**: core (shared utilities, AWS, multi-tenancy)
**Security**: auth (JWT), workspace (roles, permissions)  
**Business**: customer, product, order, invoice, tax_code, notification
**Application**: ampairs_service (main aggregator)

## Recent Changes
- 005-ampairs-backend-ampairs: Added Kotlin 2.2.20, Java 25 + Spring Boot 3.5.6, JPA/Hibernate 6.2, Flyway (already configured), MySQL JDBC Driver
- 004-create-separate-unit: Added Kotlin 2.2.20 / Java 25 + Spring Boot 3.5.6, Spring Data JPA, Hibernate 6.2, MySQL Connector

### **Multi-Timezone Support Migration (2025-01-09)**

#### **Critical Change: LocalDateTime → Instant**
- **Breaking Change**: All timestamp fields migrated from `LocalDateTime` to `java.time.Instant`
  * All new entities MUST use `Instant` for timestamps
  * All new DTOs MUST use `Instant` for date/time fields
  * Legacy `LocalDateTime` code is being phased out

#### **Base Domain Classes Updated**

#### **Frontend Impact**

### **Workspace Controller Refactoring (2025-01-15)**

#### **@TenantId Integration & Simplification**

#### **Critical Fix: @TenantId Validation Error**

#### **Repository Patterns**

### **Retail Management Platform (2025-01-06)**

### **New API Contracts**

### **Key Entity Patterns**
```kotlin
// All entities extend for multi-tenancy
abstract class OwnableBaseDomain {
    val workspaceId: String     // Tenant isolation
    val ownerId: String         // Tenant context
    val createdAt: Instant      // ✅ UTC timestamps (migrated from LocalDateTime)
    val updatedAt: Instant      // ✅ ISO-8601 with Z suffix
}

// Standard API response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?,
    val timestamp: Instant,     // ✅ UTC timestamp
    val path: String?,
    val traceId: String?
)
```

### **Business Rules**

## Build Commands

```bash
# Build all modules
./gradlew build

# Run main application  
./gradlew :ampairs_service:bootRun

# Run tests
./gradlew test
```

## Key Rules Summary

1. **Use `Instant` for all timestamps** - NEVER use `LocalDateTime` (causes timezone bugs)
2. **Use global snake_case configuration** - no redundant @JsonProperty annotations
3. **Angular M3 components only** - no other UI frameworks
4. **@EntityGraph for efficient loading** - avoid N+1 queries
5. **Follow existing patterns** - maintain consistency across modules
6. **Multi-tenant aware** - all data operations include tenant context
7. **@TenantId at controller level** - set tenant context before repository injection
8. **Native SQL for cross-tenant** - bypass @TenantId filtering when needed
9. **Single security approach** - use either @TenantId OR workspaceId parameters, not both
10. **DTO pattern required** - never expose JPA entities in controllers, always use proper DTOs
