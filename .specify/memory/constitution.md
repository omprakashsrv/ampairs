<!--
Sync Impact Report
===================
Version Change: 1.0.0 → 1.1.0
Type: MINOR (expanded alignment with repository practices)
Modified Principles: I-XI
Added Sections:
  - Project Topology
  - Testing & Quality Gates
  - Tooling & Automation
Templates Requiring Updates:
  ✅ plan-template.md - Constitution Check references CI commands
  ✅ spec-template.md - Capture M3/Compose requirements
  ✅ tasks-template.md - Reflect updated workflow enforcement
Follow-up TODOs: None
-->

# Ampairs Constitution

## Project Topology

- Ampairs comprises three first-class applications: `ampairs-backend` (Spring Boot + Kotlin), `ampairs-web` (Angular 20 + Material Design 3), and `ampairs-mp-app` (Compose Multiplatform for Android, iOS, Desktop).
- Shared tooling and automation live in `.github/workflows/`, `scripts/`, and `templates/`; generated artifacts remain under `build/` and `logs/` only.
- The runnable backend entrypoint lives in `ampairs-backend/ampairs_service/src/main/kotlin`; domain modules (`core`, `auth`, `workspace`, `customer`, `product`, `order`, `invoice`, `tax_code`, `notification`) expose their own bounded contexts.
- Compose shared business logic resides in `ampairs-mp-app/shared/src/commonMain`; launcher-specific overrides stay under each target (`androidApp`, `desktopApp`, `iosApp`).

## Core Principles

### I. Type Safety & Correctness (NON-NEGOTIABLE)

All persisted or serialized timestamps MUST use `java.time.Instant`—NEVER `LocalDateTime`. Postgres columns MUST be `TIMESTAMPTZ` to preserve timezone data.

**Rationale**: `LocalDateTime` drops timezone context, creating hidden DST issues and inconsistent API payloads. `Instant` represents a precise UTC point, aligning with industry best practice.

**Rules**:
- Entity and DTO timestamp fields MUST be `Instant`; no legacy `LocalDateTime` in new code.
- Database migrations MUST create `TIMESTAMPTZ` columns and default to `now()` at the database level if defaults are required.
- JDBC/MySQL connection strings MUST include `?serverTimezone=UTC`; Hibernate dialect SHOULD stay configured for UTC.
- JSON responses MUST emit ISO-8601 UTC strings (handled automatically when using `Instant`).

### II. DTO & Contract Isolation (NON-NEGOTIABLE)

NEVER expose JPA entities directly in API responses. ALWAYS separate request and response DTOs to protect contracts.

**Rationale**: DTO isolation enforces security (no internal/audit leaks), decouples storage schemas from public APIs, and gives clients a stable contract.

**Rules**:
- Controllers MUST accept request DTOs and return response DTOs in `domain/dto/`.
- Entity ↔ DTO conversion occurs via extension/converter functions (e.g., `entity.asEntityResponse()`).
- Request DTOs MUST include validation annotations (`@NotBlank`, `@Valid`, etc.) and capture only what the API accepts.
- Response DTOs MAY expose only client-required fields—never internal identifiers, flags, or audit columns.

### III. Global JSON Conventions Over Annotations

Leverage the global Jackson `SNAKE_CASE` configuration—do not add redundant `@JsonProperty` annotations for standard camelCase to snake_case conversions.

**Rationale**: Global naming strategy keeps DTOs clean, reduces annotation noise, and ensures parity across services and clients.

**Rules**:
- DO NOT annotate standard fields with `@JsonProperty` or `@JsonNaming`.
- Only deviate for exceptional cases and document the reason inline.
- Angular/Compose clients MUST trust the snake_case API contract; no bespoke casing transformations.

### IV. Multi-Tenant Data Isolation

Workspace context MUST be established before reaching repositories. Controllers are responsible for setting and clearing tenant scope.

**Rationale**: Tenant context validation happens at repository injection; late binding leads to mismatched tenant errors and cross-tenant data exposure.

**Rules**:
- Tenant-scoped entities MUST extend `OwnableBaseDomain`, which centralizes tenant metadata and exposes the `@TenantId ownerId` tenant identifier (mapped to the active workspace) to the tenant context infrastructure.
- Authentication filters establish tenant context post-authentication; controllers MUST verify the context before repository access and only invoke `TenantContextHolder.setCurrentTenant()` manually (wrapped in try/finally) when overriding scope.
- Services MUST NOT mutate tenant context.
- Cross-tenant administrative operations MUST use native queries (`nativeQuery = true`) with explicit safeguards.

**Client Contract**:
- Clients MUST send the `X-Workspace-ID` header on every workspace-scoped request. `SessionUserFilter` derives the current tenant from this header and rejects requests that omit it.

### V. API Response Standardization

All HTTP endpoints MUST return `ApiResponse<T>` wrappers for success and failure paths.

**Rationale**: Consistent payload envelopes simplify client integrations, observability, and debugging.

**Rules**:
- Controllers return `ApiResponse.success(data)` on success; exceptions bubble to the global handler, which emits `ApiResponse` errors.
- Standard response shape: `{"success": Boolean, "data": T?, "error": ErrorDetails?, "timestamp": Instant, "path": String?, "traceId": String?}`.
- Paginated endpoints MUST wrap data in `ApiResponse.success(PageResponse<T>)`, using `PageResponse.from(page)` (or mapped variant) to provide `content`, `page_number`, `page_size`, `total_elements`, `total_pages`, `first`, `last`, `has_next`, `has_previous`, `empty`.
- Timestamp fields inside the wrapper MUST stay aligned with Principle I.

### VI. Centralized Exception Handling

Let business exceptions bubble from controllers to the global handler, which maps them to HTTP responses.

**Rationale**: Consolidated error mapping prevents duplicated boilerplate and preserves a single formatting source of truth.

**Rules**:
- No try/catch in controllers for business exceptions.
- The global handler MUST translate domain exceptions to appropriate status codes and `ApiResponse` error bodies.
- Services and repositories MUST throw meaningful typed exceptions; avoid returning nulls for exceptional states.

### VII. Efficient Data Loading

Use `@EntityGraph` with named attribute nodes to prevent N+1 queries while keeping repositories declarative.

**Rationale**: `@EntityGraph` is reusable, composable, and avoids scattering `JOIN FETCH` logic across queries.

**Rules**:
- Define `@NamedEntityGraph` on entities for common relationship bundles.
- Reference graphs via `@EntityGraph("Entity.graphName")` on repository methods.
- Prefer derived query methods; resort to custom `@Query` only when the method name cannot express intent.
- Avoid `JOIN FETCH` when an entity graph can represent the same fetch plan.

### VIII. Angular Material 3 Exclusivity

The web application MUST rely solely on Angular Material 3 for UI components, theming, and iconography.

**Rationale**: A single design system keeps the UX cohesive, accessible, and lightweight.

**Rules**:
- Components import only from `@angular/material`; no Bootstrap, Tailwind, PrimeNG, Ant Design, or equivalent frameworks.
- Component selectors use the `amp-` prefix; templates/styles co-locate (`*.ts`, `*.html`, `*.scss`).
- Themes use Material 3 color tokens and support light/dark modes.
- Icons come from Material Design Icons.

### IX. Domain-Driven Module Boundaries

Respect bounded contexts across backend modules; keep cross-module interactions explicit.

**Rationale**: Clear module ownership lowers coupling, eases testing, and preserves team autonomy.

**Rules**:
- Package layout: `com.ampairs.{module}.{layer}`; layers include `domain`, `repository`, `service`, `controller`.
- DTOs are Kotlin `data class` implementations.
- Entities extend `BaseDomain` (system-wide) or `OwnableBaseDomain` (tenant-scoped).
- Cross-module access goes through public service interfaces, not repositories or entities.
- Each module MUST be independently testable and deployable within the overall service.

### X. Compose Multiplatform Parity

Compose shared UI MUST live in `shared/src/commonMain` with platform overrides only when necessary.

**Rationale**: Centralizing UI logic maximizes reuse across Android, iOS, and Desktop while keeping platform code thin.

**Rules**:
- Shared business flows, view models, and design tokens reside in `commonMain`.
- Platform modules (`androidApp`, `desktopApp`, `iosApp`) SHOULD remain thin launchers delegating to shared code.
- Follow Material design guidelines where available on Compose; document any platform deviations.
- Feature parity across platforms MUST be tracked in specs before merge.

### XI. Security & Secrets Hygiene

Configuration secrets MUST never live in source control; rely on environment variables and sample placeholders only.

**Rationale**: Central security posture depends on environment separation and audited secrets management.

**Rules**:
- Use environment variables (`SPRING_PROFILES_ACTIVE`, `RECAPTCHA_ENABLED`, `BUCKET4J_ENABLED`, etc.) and keep `keys/` redacted.
- Provision local dependencies via `docker-compose.yml`; do not introduce ad-hoc scripts that bypass existing tooling.
- JWT secrets, database credentials, SMTP keys, and third-party tokens live in GitHub Actions secrets or local `.env` files ignored by Git.

## Architecture Standards

### REST API Design

- Versioned routes follow `/api/v1/{resource}` or `/{module}/v1/{resource}`.
- Use appropriate HTTP verbs and status codes (200, 201, 204, 400, 401, 403, 404, 409, 422, 500).
- Collections MUST provide pagination metadata; endpoints document filtering/sorting contracts in specs.
- Authentication leverages JWT with workspace scoping; device identifiers travel within token claims.

### Backend Service Design

- Main application assembles in `ampairs_service`; feature modules remain decoupled.
- Services enforce business logic; controllers only translate HTTP to DTO/service calls.
- Repositories remain persistence-only; no business logic.
- Use constructor injection and immutable DTOs.

### Web Application

- Angular modules group features under `src/app/{feature}` with shared UI in `src/app/shared`.
- ESLint (`npm run lint`) and Angular CLI formatting guard structure; never bypass lint fixes without justification.
- Use Material theming utilities for typography, color, and density; keep styles scoped (`:host` selectors) to avoid global leaks.

### Compose Multiplatform Application

- Shared resources (strings, colors) stay in `commonMain`; avoid duplicating constants in platform modules.
- Platform launchers configure only platform-specific APIs (e.g., Android activity, iOS UIViewController).
- Desktop targets respect window sizing and theming parity with mobile.

### Database & Multi-Tenancy

- Hibernate naming strategy converts Kotlin camelCase to underscore_case automatically; do not override manually.
- Tenant-scoped entities extend `OwnableBaseDomain`, inheriting the `@TenantId ownerId` column that binds rows to the current workspace.
- Use `@EntityGraph` for relationship loading (see Principle VII).
- Migration scripts MUST remain idempotent and live alongside module-specific plans when necessary.

### Flyway Migrations

- All schema changes (DDL and data backfills) MUST ship through Flyway; never enable Hibernate auto-DDL beyond `ddl-auto: validate` outside of isolated tests.
- Place versioned migrations under each module’s `src/main/resources/db/migration` directory and follow the `V{semver}__description.sql` naming pattern (e.g., `V1.2.0__add_invoice_due_date.sql`); do not modify applied migrations—write a new version instead.
- Use Flyway tasks to manage lifecycle: `./gradlew :ampairs_service:flywayInfo` (status), `flywayValidate` (checksum verification), `flywayMigrate` (apply), and `flywayBaseline` only when onboarding an existing schema.
- Document intentional no-op changes in `NO_MIGRATION_NEEDED.md` and include rollback guidance or remediation notes within migration comments for operational traceability.

### Multi-Device Authentication

- JWT tokens include `device_id` claims for session management.
- Multiple concurrent logins per user/device pair are supported; refresh tokens are device-scoped.
- OAuth2-compatible refresh flows handle token rotation and logout per device.

## Testing & Quality Gates

- Root orchestration commands: `./gradlew buildAll`, `./gradlew testAll`, `./gradlew ciBuild` (tests execute before builds).
- Backend: `cd ampairs-backend && ./gradlew test`; keep Docker running for Testcontainers.
- Web: `cd ampairs-web && npm test`; run `npm run test:e2e:headless` for Cypress coverage before merging UI/API changes; lint with `npm run lint`.
- Multiplatform: `cd ampairs-mp-app && ./gradlew check`; add `desktopTest`, `iosTest`, or platform-specific suites when touching native bridges.
- Target coverage: backend critical logic ≥80%, API endpoints ≥90%; document gaps in plan/task templates.
- CI MUST stay green before merge; failures block promotion.

## Development Workflow

### Code Organization

- Backend modules sit under `ampairs-backend/{module}/src/main/kotlin/com/ampairs/{module}/`.
- Shared backend utilities belong in the `core` module; multi-tenancy infrastructure stays in `workspace`.
- Web components co-locate `.ts/.html/.scss`; selectors follow `amp-{feature}`; keep classes `PascalCase`.
- Compose shared logic stays in `shared/`; platform overrides live under each launcher's `src`.

### Branching & Commits

- Feature branches follow `###-feature-name` (e.g., `123-auth-mfa`); experimental work may use `spike/###-topic`.
- Use Conventional Commits (`feat:`, `fix:`, `refactor:`, etc.); subjects ≤72 characters and reference issues (e.g., `AMP-123`) in bodies.
- Commit after each logical unit of work; do not batch unrelated changes.

### Review & CI Expectations

- Pull requests MUST describe scope, affected modules, and validation commands.
- Attach screenshots or API diffs when behavior changes.
- Request reviewers from the owning squad (backend, web, mobile).
- Ensure `./gradlew ciBuild`, Angular lint/tests, and multiplatform checks pass before requesting merge.

### Documentation Requirements

- Feature specs live in `/specs/{###-feature}/spec.md`; implementation plans in `plan.md`.
- API contracts belong in `/specs/{###-feature}/contracts/`; data models in `data-model.md`; quickstarts in `quickstart.md`.
- Update `CLAUDE.md` and `AGENTS.md` when introducing new architectural patterns or rules.

## Tooling & Automation

- Preferred orchestration: `./gradlew buildAll`, `testAll`, `cleanAll`, `ciBuild`.
- Backend bootstrapping: `cd ampairs-backend && ./gradlew bootRun`; use `SPRING_PROFILES_ACTIVE=test` for E2E flows.
- Web startup: `cd ampairs-web && npm install && npm start`; production bundles via `npm run build:prod`.
- Multiplatform launchers: `cd ampairs-mp-app && ./gradlew run`, `installDebug`, `package`.
- Local dependencies load via `docker-compose.yml`; extend scripts under `scripts/` when automation is required—never commit ad-hoc tooling elsewhere.
- Use `start-dev.sh` / `start-test.sh` for curated environments; keep them updated when dependencies change.

## Governance

### Amendment Procedure

This constitution supersedes other practice guides. Amendments require:

1. Documentation of proposed changes with rationale.
2. Impact analysis across modules and templates.
3. Team approval (or architect approval in solo work).
4. Migration plan for breaking changes.
5. Updates to dependent templates and documentation.

### Versioning Policy

- Uses semantic versioning (`MAJOR.MINOR.PATCH`).
- **MAJOR**: Breaking governance changes or principle redefinitions.
- **MINOR**: New principles or materially expanded guidance.
- **PATCH**: Clarifications, wording fixes, or typo corrections.

### Compliance Review

- All pull requests MUST verify compliance with constitution principles.
- Deviations require documented justification in `plan.md` under Complexity Tracking.
- Templates MUST stay aligned with the constitution.
- Review the constitution at least quarterly to confirm ongoing relevance.

### Living Document

The constitution evolves with real-world lessons. When new patterns emerge, encode them through the amendment procedure and document knowledge in specs and `CLAUDE.md`.

**Version**: 1.1.0 | **Ratified**: 2025-10-11 | **Last Amended**: 2025-10-11
