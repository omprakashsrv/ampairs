# Implementation Plan: Database Schema Migration with Flyway

**Branch**: `005-ampairs-backend-ampairs` | **Date**: 2025-10-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-ampairs-backend-ampairs/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Create comprehensive Flyway migration scripts for all existing JPA entities across core, customer, product, order, invoice, unit, and form modules to enable production deployment with `ddl-auto: validate` configuration. The feature establishes version-controlled database schema management following the patterns established by existing tax (V3_1) and event (V3_2) migrations, ensuring all 22+ entities have corresponding DDL scripts that match JPA entity definitions exactly.

## Technical Context

**Language/Version**: Kotlin 2.2.20, Java 25
**Primary Dependencies**: Spring Boot 3.5.6, JPA/Hibernate 6.2, Flyway (already configured), MySQL JDBC Driver
**Storage**: MySQL 8.0+ with UTC timezone configuration (`serverTimezone=UTC`)
**Testing**: Testcontainers with MySQL for integration tests, SpringBootTest for migration validation
**Target Platform**: JVM server (production/staging/development environments)
**Project Type**: Backend multi-module Spring Boot application
**Performance Goals**: Schema creation completes in under 30 seconds on standard hardware, zero schema validation errors
**Constraints**: Must maintain backward compatibility with existing V3_1/V3_2 migrations, must support `@TenantId` multi-tenant architecture, must use MySQL-specific syntax
**Scale/Scope**: 22+ entities across 6 modules (customer: 6, product: 10, order: 2, invoice: 2, unit: 2, form: 2), estimated 15-20 migration files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Type Safety & Correctness (NON-NEGOTIABLE)
**Status**: ✅ **COMPLIANT**
- All migration scripts will create `TIMESTAMP` columns (not `DATETIME`) for timestamp fields
- Aligns with existing entity definitions using `java.time.Instant`
- MySQL JDBC connection already configured with `?serverTimezone=UTC`
- No changes to entity timestamp types required

### Principle II: DTO & Contract Isolation (NON-NEGOTIABLE)
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- This feature focuses on database schema (DDL) only
- No API contracts or DTOs are modified
- Existing DTO patterns remain unchanged

### Principle III: Global JSON Conventions Over Annotations
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- No JSON serialization changes
- Feature is database schema only

### Principle IV: Multi-Tenant Data Isolation
**Status**: ✅ **COMPLIANT**
- All migrations will include `owner_id VARCHAR(200)` for entities extending `OwnableBaseDomain`
- Migrations respect existing `@TenantId` architecture
- Tables will include workspace_id columns where entities are tenant-scoped
- No changes to tenant context handling required

### Principle V: API Response Standardization
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- No API responses modified
- Feature is database schema only

### Principle VI: Centralized Exception Handling
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- No exception handling changes
- Flyway migration failures will propagate through Spring Boot's standard error handling

### Principle VII: Efficient Data Loading
**Status**: ✅ **COMPLIANT**
- Foreign key constraints in migrations will match existing `@ManyToOne`, `@OneToOne` relationships
- No changes to `@EntityGraph` or repository patterns
- Migration scripts will create indexes matching entity annotations

### Principle VIII: Angular Material 3 Exclusivity
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- Backend-only feature
- No web UI changes

### Principle IX: Domain-Driven Module Boundaries
**Status**: ✅ **COMPLIANT**
- Migrations grouped by module (customer, product, order, invoice, unit, form)
- Each migration file corresponds to a specific module's bounded context
- Dependency order respected: core → unit → tax → customer → product → order → invoice → form

### Principle X: Compose Multiplatform Parity
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- Backend-only feature
- No mobile app changes

### Principle XI: Security & Secrets Hygiene
**Status**: ✅ **COMPLIANT**
- No secrets or credentials in migration files
- Database connection details remain in environment variables
- Migration scripts contain only DDL statements

### Flyway Migrations Standard (Constitution § Database & Multi-Tenancy)
**Status**: ✅ **COMPLIANT**
- All schema changes delivered through Flyway versioned migrations
- Application configured with `ddl-auto: validate` (line 151 of application.yml)
- Migrations placed in each owning module under `src/main/resources/db/migration/mysql/` so Flyway can load them via the module classpath
- Naming pattern: `V{major}_{minor}__{description}.sql` (e.g., `V4_1__create_customer_module_tables.sql`)
- Existing migrations (V3_1 tax, V3_2 event) will not be modified
- New migrations continue sequential numbering from V4_1 onwards

### Multi-Device Authentication Standard
**Status**: ✅ **COMPLIANT - NOT APPLICABLE**
- No authentication changes
- Migrations will preserve existing device_id columns if present in entities

**Overall Constitution Compliance**: ✅ **FULLY COMPLIANT** - All applicable principles satisfied, no violations requiring justification.

## Project Structure

### Documentation (this feature)

```
specs/005-ampairs-backend-ampairs/
├── spec.md                  # Feature specification (created by /speckit.specify)
├── plan.md                  # This file (created by /speckit.plan)
├── research.md              # Phase 0 research decisions (created by /speckit.plan)
├── data-model.md            # Phase 1 entity-to-table mappings (created by /speckit.plan)
├── quickstart.md            # Phase 1 developer guide (created by /speckit.plan)
├── contracts/               # Phase 1 migration script templates (created by /speckit.plan)
│   ├── migration-template.sql
│   └── README.md
├── checklists/
│   └── requirements.md      # Specification quality checklist (already created)
└── tasks.md                 # Phase 2 task breakdown (created by /speckit.tasks - NOT by /speckit.plan)
```

### Source Code (repository root)

```
ampairs-backend/
├── ampairs_service/
│   └── src/main/resources/
│       ├── application.yml                    # Contains ddl-auto: validate config
│       └── db/migration/                      # Aggregated documentation (no module SQL)
│           ├── MIGRATION_BASELINE.md
│           └── README.md
│
├── auth/src/main/resources/db/migration/
│   ├── mysql/V4_8__create_auth_module_tables.sql
│   └── postgresql/V4_8__create_auth_module_tables.sql
│
├── core/src/main/resources/db/migration/mysql/
│   └── V4_1__create_core_tables.sql
│
├── unit/src/main/resources/db/migration/mysql/
│   └── V4_2__create_unit_module_tables.sql
│
├── customer/src/main/resources/db/migration/mysql/
│   └── V4_3__create_customer_module_tables.sql
│
├── product/src/main/resources/db/migration/mysql/
│   └── V4_4__create_product_module_tables.sql
│
├── order/src/main/resources/db/migration/mysql/
│   └── V4_5__create_order_module_tables.sql
│
├── invoice/src/main/resources/db/migration/mysql/
│   └── V4_6__create_invoice_module_tables.sql
│
├── unit/src/main/kotlin/com/ampairs/unit/domain/model/
│   ├── Unit.kt                                # 2 entities requiring migrations
│   └── UnitConversion.kt
│
├── form/src/main/resources/db/migration/mysql/
│   └── V4_7__create_form_module_tables.sql
│
└── form/src/main/kotlin/com/ampairs/form/domain/model/
│
├── core/src/main/kotlin/com/ampairs/core/domain/model/
│   ├── BaseDomain.kt                          # Base entity (id, uid, createdAt, updatedAt)
│   ├── OwnableBaseDomain.kt                   # Tenant-scoped base (+ workspaceId, ownerId)
│   ├── File.kt                                # File entity
│   ├── Address.kt                             # Address entity
│   └── AbstractIdVerification.kt              # Verification entity
│
├── customer/src/main/kotlin/com/ampairs/customer/domain/model/
│   ├── Customer.kt                            # 6 entities requiring migrations
│   ├── CustomerGroup.kt
│   ├── CustomerType.kt
│   ├── CustomerImage.kt
│   ├── State.kt
│   └── MasterState.kt
│
├── product/src/main/kotlin/com/ampairs/product/domain/model/
│   ├── Product.kt                             # 10 entities requiring migrations
│   ├── ProductImage.kt
│   ├── ProductPrice.kt
│   └── [7 more entities including ProductGroup, ProductCategory, etc.]
│
├── product/src/main/kotlin/com/ampairs/inventory/domain/model/
│   ├── Inventory.kt
│   ├── InventoryTransaction.kt
│   └── InventoryUnitConversion.kt
│
├── order/src/main/kotlin/com/ampairs/order/domain/model/
│   ├── Order.kt                               # 2 entities requiring migrations
│   └── OrderItem.kt
│
├── invoice/src/main/kotlin/com/ampairs/invoice/domain/model/
│   ├── Invoice.kt                             # 2 entities requiring migrations
│   └── InvoiceItem.kt
│
└── form/src/main/kotlin/com/ampairs/form/domain/model/
    ├── AttributeDefinition.kt                 # 2 entities requiring migrations
    └── FieldConfig.kt
```

**Structure Decision**: Ampairs follows a multi-module monorepo structure where each domain (auth, core, customer, product, order, invoice, unit, form, tax, event) is a separate Gradle module with its own JPA entities. The main application module (`ampairs_service`) aggregates all domain modules and loads their resources from the classpath. Migrations now live alongside their owning module (`{module}/src/main/resources/db/migration/mysql/`), mirroring the patterns used by `business` and `workspace`.

## Complexity Tracking

*No Constitution violations - this section is empty.*

All constitution principles are satisfied:
- ✅ Type Safety: TIMESTAMP columns for all timestamps
- ✅ Multi-Tenant: owner_id and workspace_id columns for tenant-scoped entities
- ✅ Flyway Standard: Versioned migrations in standard location
- ✅ Module Boundaries: Migrations grouped by module, respecting dependencies
- ✅ No implementation details in spec: plan.md contains technical details only

**No complexity justifications required.**
