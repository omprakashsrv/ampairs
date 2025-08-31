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

## Module Structure

**Foundation**: core (shared utilities, AWS, multi-tenancy)
**Security**: auth (JWT), workspace (roles, permissions)  
**Business**: customer, product, order, invoice
**Application**: ampairs_service (main aggregator)

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