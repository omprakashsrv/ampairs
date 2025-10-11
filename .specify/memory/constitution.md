<!--
Sync Impact Report
===================
Version Change: N/A (initial constitution) → 1.0.0
Type: MAJOR (initial ratification)
Modified Principles: N/A (initial creation)
Added Sections:
  - Core Principles (I-IX)
  - Architecture Standards
  - Development Workflow
  - Governance
Templates Requiring Updates:
  ✅ plan-template.md - Constitution Check section references this file
  ✅ spec-template.md - Requirements align with constitution principles
  ✅ tasks-template.md - Task categorization reflects principle-driven task types
Follow-up TODOs: None
-->

# Ampairs Constitution

## Core Principles

### I. Type Safety & Correctness (NON-NEGOTIABLE)

All timestamps MUST use `java.time.Instant` - NEVER `LocalDateTime`. This prevents timezone ambiguity bugs during DST transitions and ensures unambiguous point-in-time representation across all timezones.

**Rationale**: LocalDateTime has no timezone information, causing silent bugs when data crosses timezone boundaries. Instant represents a specific point on the UTC timeline, eliminating ambiguity. This is a non-negotiable standard used by AWS, Google, GitHub, and Stripe.

**Rules**:
- All entity timestamp fields MUST use `Instant`
- All DTO timestamp fields MUST use `Instant`
- Database columns MUST be `TIMESTAMP` (not `DATETIME`)
- Connection strings MUST include `?serverTimezone=UTC`
- Legacy `LocalDateTime` code is being actively migrated and MUST NOT be used in new features

### II. Global Conventions Over Annotations

Leverage global Jackson `SNAKE_CASE` configuration for property naming - NEVER add redundant `@JsonProperty` annotations for standard camelCase-to-snake_case conversions.

**Rationale**: Spring Boot's global configuration handles naming strategy automatically, eliminating annotation clutter and reducing maintenance burden. Annotations should only be used for exceptional cases that deviate from the standard pattern.

**Rules**:
- NO `@JsonProperty` annotations for standard camelCase → snake_case mappings
- Let global `spring.jackson.property-naming-strategy: SNAKE_CASE` handle conversion
- Only use `@JsonProperty` for special cases that don't follow standard pattern
- Document any exception to this rule with inline comments explaining why

### III. Data Transfer Object (DTO) Pattern (NON-NEGOTIABLE)

NEVER expose JPA entities directly in API responses. ALWAYS use Request DTOs for inputs and Response DTOs for outputs.

**Rationale**: Direct entity exposure creates security vulnerabilities (exposing internal fields), tight coupling (database schema changes force API changes), and prevents API evolution. DTOs provide a controlled API contract layer.

**Rules**:
- Controllers MUST use Response DTOs for all API outputs
- Controllers MUST use Request DTOs for all API inputs
- DTOs MUST be in `domain/dto/` package
- Entity-to-DTO conversion via extension functions (e.g., `entity.asEntityResponse()`)
- Request DTOs MUST include validation annotations (`@NotBlank`, `@Valid`, etc.)
- Response DTOs MUST only expose fields clients need - NO internal/audit fields

### IV. Multi-Tenant Data Isolation

All tenant-scoped operations MUST respect workspace context. Set tenant context at controller level BEFORE any repository operations.

**Rationale**: Tenant context timing is critical - `@TenantId` validation occurs at repository injection time. Setting context in service layers causes "assigned tenant id differs from current tenant id" errors.

**Rules**:
- Add `@TenantId` annotation to entity tenant identifier fields (e.g., `workspaceId`)
- Controllers MUST set tenant context using `TenantContextHolder.setCurrentTenant()` BEFORE repository calls
- Use try-finally blocks to ensure tenant context cleanup
- Service layers MUST NOT handle tenant context switching
- Use native SQL queries (`nativeQuery = true`) to bypass `@TenantId` filtering for cross-tenant operations (e.g., workspace listing, invitations)
- Single security approach: use EITHER `@TenantId` filtering OR `workspaceId` parameters, NOT both

### V. Efficient Data Loading

Use `@EntityGraph` with named attribute nodes for efficient relationship loading - AVOID `JOIN FETCH` in JPQL queries.

**Rationale**: `@EntityGraph` prevents N+1 queries while maintaining clean separation between entity definitions and data access patterns. It's reusable across multiple repository methods and more maintainable than scattered JOIN FETCH queries.

**Rules**:
- Define `@NamedEntityGraph` at entity level with relationship attribute nodes
- Repository methods MUST use `@EntityGraph` annotation referencing named graphs
- Use EXISTS subqueries for filtering with entity graphs
- Prefer Spring Data JPA derived query methods (`findByActiveTrueOrderByName()`)
- Custom `@Query` only for complex business logic that cannot be expressed via method names
- NO `JOIN FETCH` when `@EntityGraph` achieves the same result

### VI. Centralized Exception Handling

Let exceptions bubble up from controllers - global exception handler converts them to standardized HTTP responses.

**Rationale**: Controllers should focus on HTTP concerns (request/response mapping), not exception-to-HTTP mapping. Centralized handling ensures consistent error response format across all endpoints and reduces controller boilerplate.

**Rules**:
- NEVER use try-catch blocks in controllers for business logic exceptions
- Let service layer exceptions propagate to global exception handler
- Global handler MUST return `ApiResponse<T>` with error details
- All controller methods return `ApiResponse<T>` wrapper for consistency
- Use `ApiResponse.success(data)` for successful responses
- Exception handler maps exception types to appropriate HTTP status codes

### VII. Angular Material Design 3 (M3) Exclusively

Web frontend MUST use Angular Material 3 components exclusively - NO Bootstrap, Tailwind CSS, PrimeNG, Ant Design, or custom CSS frameworks.

**Rationale**: Material Design 3 provides comprehensive, accessible, themeable components with consistent UX patterns. Multiple UI frameworks create maintenance burden, larger bundle sizes, conflicting styles, and inconsistent user experience.

**Rules**:
- ALL UI components MUST be from `@angular/material` package
- Theme system MUST use Material Design 3 color tokens
- Icons MUST be Material Design Icons only
- Support light/dark mode via M3 theme system
- NO imports from Bootstrap, Tailwind, PrimeNG, Ant Design, or similar frameworks
- Custom components MUST follow Material Design 3 guidelines

### VIII. API Response Standardization

All controller endpoints MUST return `ApiResponse<T>` wrapper with consistent structure for success and error responses.

**Rationale**: Consistent response format simplifies client-side error handling, provides consistent metadata (timestamps, trace IDs), and enables centralized response transformation/logging.

**Rules**:
- ALL controller methods MUST return `ApiResponse<T>`
- Import `com.ampairs.core.domain.dto.ApiResponse`
- Success responses: `ApiResponse.success(data)`
- Error responses: handled by global exception handler returning `ApiResponse` with error details
- Response structure: `{"success": Boolean, "data": T?, "error": ErrorDetails?, "timestamp": Instant, "path": String?, "traceId": String?}`
- Timestamp field MUST use `Instant` type (see Principle I)

### IX. Domain-Driven Design & Module Boundaries

Each module represents a bounded context with clear responsibility boundaries. Follow existing package structure and separation of concerns.

**Rationale**: Domain-driven design prevents tangled dependencies, enables independent module evolution, and makes the system easier to understand and maintain. Clear boundaries reduce cognitive load and testing complexity.

**Rules**:
- Follow package structure: `com.ampairs.{module}.{layer}`
- Module layers: `domain` (entities, DTOs), `repository`, `service`, `controller`
- Use Kotlin data classes for DTOs
- Extend `BaseDomain` or `OwnableBaseDomain` for entities
- Maintain separation of concerns: controllers handle HTTP, services handle business logic, repositories handle data access
- Cross-module dependencies MUST go through public service interfaces, NOT repositories
- Each module MUST be independently testable

## Architecture Standards

### REST API Design

- Endpoints MUST follow pattern: `/api/v1/{resource}` or `/{module}/v1/{resource}`
- Use proper HTTP status codes: 200 (OK), 201 (Created), 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 500 (Internal Server Error)
- List endpoints MUST support pagination
- Use proper HTTP verbs: GET (read), POST (create), PUT (update), PATCH (partial update), DELETE (delete)

### Database Design

- Entity field naming: camelCase (Kotlin) → underscore_case (database) via Hibernate naming strategy
- Entities MUST extend `BaseDomain` (system entities) or `OwnableBaseDomain` (tenant-scoped entities)
- Base domain fields: `uid` (primary key), `createdAt`, `updatedAt` (all Instant type)
- Ownable base domain adds: `workspaceId`, `ownerId` for multi-tenancy
- Use `@EntityGraph` for efficient relationship loading (see Principle V)

### Multi-Device Authentication

- JWT tokens MUST include `device_id` for session management
- Support multiple concurrent logins per user across different devices
- Device-specific refresh tokens enable per-device logout
- Token expiry and refresh handled via standard OAuth2 flows

### Testing Standards

- Test organization: `contract/` (API contract tests), `integration/` (cross-layer tests), `unit/` (isolated tests)
- Integration tests for: new modules, contract changes, inter-service communication, shared schemas
- Test coverage goals: critical business logic >80%, API endpoints >90%
- Use appropriate test frameworks: JUnit 5, MockK (Kotlin mocking), TestContainers (database tests)

## Development Workflow

### Code Organization

- Module structure: `ampairs-backend/{module}/src/main/kotlin/com/ampairs/{module}/`
- Shared utilities in `core` module
- Multi-tenancy infrastructure in `workspace` module
- Authentication in `auth` module
- Business domains: `customer`, `product`, `order`, `invoice`, `tax_code`, `notification`
- Main application aggregator: `ampairs_service`

### Build & Deployment

- Build all modules: `./gradlew build`
- Run main application: `./gradlew :ampairs_service:bootRun`
- Run tests: `./gradlew test`
- CI/CD: automated build, test, JAR creation, SSH deployment to Ubuntu server, service restart, health verification on push to `main` branch

### Version Control

- Feature branches: `###-feature-name` pattern
- Commit after each logical task or task group
- Pull requests MUST pass CI build and tests before merge
- Code reviews required for all changes

### Documentation Requirements

- Feature specifications in `/specs/{###-feature}/spec.md`
- Implementation plans in `/specs/{###-feature}/plan.md`
- API contracts in `/specs/{###-feature}/contracts/`
- Data models documented in `/specs/{###-feature}/data-model.md`
- Quickstart guides in `/specs/{###-feature}/quickstart.md`
- Update `CLAUDE.md` for new architectural patterns or critical rules

## Governance

### Amendment Procedure

This constitution supersedes all other development practices. Amendments require:

1. Documentation of proposed changes with clear rationale
2. Impact analysis across affected modules and templates
3. Team approval (for team environments) or architect approval (for solo projects)
4. Migration plan for existing code if breaking changes introduced
5. Update of all dependent templates and documentation

### Versioning Policy

Constitution follows semantic versioning (`MAJOR.MINOR.PATCH`):

- **MAJOR**: Backward-incompatible governance changes, principle removals, or principle redefinitions
- **MINOR**: New principles added or materially expanded guidance
- **PATCH**: Clarifications, wording improvements, typo fixes, non-semantic refinements

### Compliance Review

- All pull requests MUST verify compliance with constitution principles
- Complexity violations MUST be justified in `plan.md` Complexity Tracking section
- Template updates MUST maintain alignment with constitution principles
- Periodic constitution review (quarterly recommended) to ensure continued relevance

### Living Document

This constitution is a living document that evolves with the project. When principles are found insufficient or incorrect based on real-world experience, they should be amended through proper governance channels. Document lessons learned and encode them as principles when patterns emerge.

**Version**: 1.0.0 | **Ratified**: 2025-10-11 | **Last Amended**: 2025-10-11