# Implementation Plan: Separate Unit Module

**Branch**: `004-create-separate-unit` | **Date**: 2025-10-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-create-separate-unit/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Extract unit management functionality from the product module into a dedicated unit module following the same architectural pattern established by the tax module extraction. This creates a centralized, reusable unit catalog that supports measurement units and unit conversions across product, inventory, and invoice modules. The extraction maintains 100% feature parity while improving module boundaries, reducing coupling, and enabling independent evolution of unit management capabilities.

## Technical Context

**Language/Version**: Kotlin 2.2.20 / Java 25
**Primary Dependencies**: Spring Boot 3.5.6, Spring Data JPA, Hibernate 6.2, MySQL Connector
**Storage**: MySQL 8.0+ with TIMESTAMP columns (UTC timezone), managed via Hibernate/JPA
**Testing**: JUnit 5 (JUnit Platform), Mockito Kotlin, Spring Boot Test, H2 (in-memory for tests)
**Target Platform**: JVM (Spring Boot embedded Tomcat), deployed as library module (not standalone service)
**Project Type**: Backend module library (part of multi-module Spring Boot application)
**Performance Goals**: API response times <200ms, support 10,000+ concurrent users, efficient N+1 query prevention via @EntityGraph
**Constraints**: Zero downtime deployment, backward-compatible database migrations, multi-tenant data isolation, no breaking API changes
**Scale/Scope**: New backend module with ~15-20 classes (entities, DTOs, repositories, services, controllers), migration of existing unit data from product module, integration with 3+ dependent modules (product, inventory, invoice)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Type Safety & Correctness
- ✅ **PASS**: All timestamp fields will use `java.time.Instant` (Unit and UnitConversion entities inherit from OwnableBaseDomain which uses Instant)
- ✅ **PASS**: Database columns will be `TIMESTAMP` (no new timestamp columns needed; inherited from BaseDomain)
- ✅ **PASS**: MySQL connection strings already include `?serverTimezone=UTC`

### Principle II: DTO & Contract Isolation
- ✅ **PASS**: Will create separate Request/Response DTOs (UnitRequest, UnitResponse, UnitConversionRequest, UnitConversionResponse)
- ✅ **PASS**: Controllers will never expose JPA entities directly
- ✅ **PASS**: Extension functions will handle entity ↔ DTO conversion (e.g., `Unit.asUnitResponse()`)

### Principle III: Global JSON Conventions
- ✅ **PASS**: Will rely on global Jackson SNAKE_CASE configuration
- ✅ **PASS**: No redundant @JsonProperty annotations for standard camelCase to snake_case conversions

### Principle IV: Multi-Tenant Data Isolation
- ✅ **PASS**: Unit entities extend OwnableBaseDomain (includes @TenantId ownerId)
- ✅ **PASS**: Controllers will establish tenant context before repository access
- ✅ **PASS**: Services will not mutate tenant context
- ✅ **PASS**: All API endpoints will require X-Workspace-ID header

### Principle V: API Response Standardization
- ✅ **PASS**: All endpoints will return `ApiResponse<T>` wrapper
- ✅ **PASS**: Success responses will use `ApiResponse.success(data)`
- ✅ **PASS**: Paginated endpoints will use `ApiResponse.success(PageResponse<T>)`

### Principle VI: Centralized Exception Handling
- ✅ **PASS**: Controllers will not contain try/catch for business exceptions
- ✅ **PASS**: Global exception handler will map exceptions to HTTP responses

### Principle VII: Efficient Data Loading
- ✅ **PASS**: Will use @EntityGraph with @NamedEntityGraph for unit conversion relationships
- ✅ **PASS**: Will prefer derived query methods in repositories
- ✅ **PASS**: Will avoid JOIN FETCH in favor of @EntityGraph

### Principle VIII: Angular Material 3 Exclusivity
- ✅ **N/A**: Backend module only, no frontend changes

### Principle IX: Domain-Driven Module Boundaries
- ✅ **PASS**: Package structure will follow `com.ampairs.unit.{layer}` pattern
- ✅ **PASS**: Entities will extend OwnableBaseDomain for tenant scoping
- ✅ **PASS**: Other modules will access unit module through service interfaces, not repositories
- ✅ **PASS**: Module will be independently testable and deployable

### Principle X: Compose Multiplatform Parity
- ✅ **N/A**: Backend module only

### Principle XI: Security & Secrets Hygiene
- ✅ **PASS**: No secrets in source control
- ✅ **PASS**: All configuration via environment variables or Spring profiles

### Architecture Standards
- ✅ **PASS**: REST API routes will follow `/api/v1/unit` pattern
- ✅ **PASS**: Services enforce business logic; controllers only translate HTTP
- ✅ **PASS**: Repositories remain persistence-only
- ✅ **PASS**: Constructor injection and immutable DTOs

### Flyway Migrations
- ✅ **PASS**: No schema changes required (tables already exist under product module)
- ⚠️ **NOTE**: Database tables remain in place; only code moves to new module

### Testing & Quality Gates
- ✅ **PASS**: Will maintain ≥80% coverage for business logic
- ✅ **PASS**: Will add integration tests using Testcontainers or H2
- ✅ **PASS**: CI must stay green before merge

**Overall Status**: ✅ **ALL GATES PASSED** - Ready for Phase 0 research

---

## Constitution Re-Check (Post-Design)

*Re-evaluated after completing Phase 0 (research.md) and Phase 1 (data-model.md, contracts/, quickstart.md)*

### Design Artifacts Review

✅ **Data Model** (data-model.md):
- All entities use `Instant` for timestamps (Principle I)
- DTOs properly separated from entities (Principle II)
- No @JsonProperty annotations - relies on global snake_case (Principle III)
- OwnableBaseDomain provides @TenantId isolation (Principle IV)
- Entity graphs defined for efficient loading (Principle VII)

✅ **API Contracts** (contracts/unit-api.yaml):
- All responses wrapped in `ApiResponse<T>` (Principle V)
- RESTful endpoint design: `/api/v1/unit` (Architecture Standards)
- X-Workspace-ID header required (Principle IV)
- Standard HTTP status codes (Architecture Standards)
- OpenAPI 3.0.3 specification complete

✅ **Quickstart Guide** (quickstart.md):
- Service injection patterns documented (Principle IX)
- Multi-tenant considerations explained (Principle IV)
- Testing strategies align with Testing & Quality Gates
- Error handling through global exception handler (Principle VI)
- Performance tips include @EntityGraph usage (Principle VII)

### Final Assessment

All design artifacts comply with Ampairs constitution principles. No violations introduced during design phase.

**Status**: ✅ **READY FOR IMPLEMENTATION** (Phase 2: /speckit.tasks)

## Project Structure

### Documentation (this feature)

```
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```
ampairs-backend/
├── unit/                              # NEW MODULE
│   ├── build.gradle.kts               # Module build configuration
│   └── src/
│       ├── main/
│       │   └── kotlin/com/ampairs/unit/
│       │       ├── config/
│       │       │   ├── UnitModuleConfiguration.kt
│       │       │   └── Constants.kt
│       │       ├── controller/
│       │       │   ├── UnitController.kt
│       │       │   └── UnitConversionController.kt
│       │       ├── service/
│       │       │   ├── UnitService.kt
│       │       │   └── UnitConversionService.kt
│       │       ├── repository/
│       │       │   ├── UnitRepository.kt
│       │       │   └── UnitConversionRepository.kt
│       │       ├── domain/
│       │       │   ├── model/
│       │       │   │   ├── Unit.kt
│       │       │   │   └── UnitConversion.kt
│       │       │   └── dto/
│       │       │       ├── UnitRequest.kt
│       │       │       ├── UnitResponse.kt
│       │       │       ├── UnitConversionRequest.kt
│       │       │       └── UnitConversionResponse.kt
│       │       └── exception/
│       │           └── UnitNotFoundException.kt
│       └── test/
│           └── kotlin/com/ampairs/unit/
│               ├── UnitServiceTest.kt
│               ├── UnitControllerIntegrationTest.kt
│               └── UnitConversionTest.kt
│
├── product/                           # MODIFIED MODULE
│   └── src/main/kotlin/com/ampairs/product/
│       └── (remove unit-related classes, add dependency on unit module)
│
├── inventory/                         # MODIFIED MODULE (inventory subpackage in product)
│   └── src/main/kotlin/com/ampairs/inventory/
│       └── domain/model/
│           └── InventoryUnitConversion.kt  # May need refactoring to use unit module
│
├── core/                             # EXISTING DEPENDENCY
│   └── (provides OwnableBaseDomain, ApiResponse, multi-tenant infrastructure)
│
└── ampairs_service/                  # MAIN APPLICATION
    └── build.gradle.kts              # Add unit module dependency
```

**Structure Decision**:

This is a **backend module library** within the existing Spring Boot multi-module monorepo. The new `unit` module follows the same structure as the `tax` module:

1. **Module Type**: Library module (bootJar disabled, jar enabled)
2. **Package Structure**: `com.ampairs.unit.{layer}` following domain-driven design
3. **Layers**: config, controller, service, repository, domain (model + dto), exception
4. **Dependencies**:
   - Depends on `core` module for base classes and utilities
   - Product, inventory, and invoice modules will depend on `unit` module
   - Main `ampairs_service` module aggregates all feature modules
5. **Testing Strategy**: Unit tests + integration tests using H2 in-memory database

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

**Status**: No violations detected. All constitution principles are satisfied by this design.
