# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ampairs is a comprehensive business management system with **three integrated applications**:

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

### **Workspace Controller Refactoring (2025-01-15)**

#### **@TenantId Integration & Simplification**
- Eliminated dual security (workspaceId params + @TenantId filtering)
- Controllers use `TenantContextHolder.getCurrentTenant()` from X-Workspace-ID header
- Added `@TenantId` to `WorkspaceMember.workspaceId` for automatic filtering
- Endpoint paths simplified: `/workspace/v1/member` (no workspaceId params)

#### **Critical Fix: @TenantId Validation Error**
- **Issue**: `assigned tenant id differs from current tenant id` during invitation acceptance
- **Root Cause**: Tenant context set in service layer, but @TenantId validation happens at repository injection
- **Solution**: Move tenant context to controller level before any repository operations
- **Pattern**: Controller sets tenant → Service operates → Controller restores tenant

#### **Repository Patterns**
- **Tenant-Aware**: Methods automatically filtered by @TenantId annotation
- **Cross-Tenant**: Use native SQL queries to bypass @TenantId filtering
- **Column Mapping**: JPA properties → database columns (createdAt → created_at)

### **Retail Management Platform (2025-01-06)**

### **New API Contracts**
- **Workspace Management**: `/workspace/v1` - Multi-tenant business environments with role-based access
- **Product Catalog**: `/product/v1` - Product management with inventory tracking and tax codes
- **Order Processing**: `/order/v1` - Sales transactions with status workflow (DRAFT→CONFIRMED→FULFILLED)
- **Customer Management**: `/customer/v1` - Business contacts with GST compliance and credit limits
- **Invoice Generation**: `/invoice/v1` - Billing with tax calculations, payment tracking, and PDF export
- **Tax Code Management**: `/tax-code/v1` - GST compliance with component breakdown (SGST/CGST/IGST)

### **Key Entity Patterns**
```kotlin
// All entities extend for multi-tenancy
abstract class OwnableBaseDomain {
    val workspaceId: String     // Tenant isolation
    val ownerId: String         // Tenant context
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}

// Standard API response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?,
    val timestamp: LocalDateTime,
    val path: String?,
    val traceId: String?
)
```

### **Business Rules**
- **Order Status Flow**: DRAFT → CONFIRMED → PROCESSING → FULFILLED (with CANCELLED/RETURNED branches)
- **Invoice Status Flow**: DRAFT → SENT → PARTIAL_PAID → PAID (with OVERDUE/CANCELLED)
- **Inventory Reservations**: Stock reserved on CONFIRMED orders, consumed on FULFILLED
- **GST Compliance**: Tax codes support SGST+CGST (intrastate) or IGST (interstate) calculations
- **Multi-tenant Data**: All operations filtered by workspace context automatically

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

1. **Use global snake_case configuration** - no redundant @JsonProperty annotations
2. **Angular M3 components only** - no other UI frameworks
3. **@EntityGraph for efficient loading** - avoid N+1 queries
4. **Follow existing patterns** - maintain consistency across modules
5. **Multi-tenant aware** - all data operations include tenant context
6. **@TenantId at controller level** - set tenant context before repository injection
7. **Native SQL for cross-tenant** - bypass @TenantId filtering when needed
8. **Single security approach** - use either @TenantId OR workspaceId parameters, not both