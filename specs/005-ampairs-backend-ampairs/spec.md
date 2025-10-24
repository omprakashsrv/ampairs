# Feature Specification: Database Schema Migration with Flyway

**Feature Branch**: `005-ampairs-backend-ampairs`
**Created**: 2025-10-12
**Status**: Draft
**Input**: User description: "@ampairs-backend/ampairs_service/src/main/resources/application.yml#L145-151 we are not having auto ddl. but for all existing entities we have not added the schema creation sql using flyway."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate Missing Flyway Migration Scripts (Priority: P1)

As a developer, I need comprehensive Flyway migration scripts for all existing JPA entities so that the database schema is version-controlled and the application can start successfully with `ddl-auto: validate` in production environments.

**Why this priority**: This is the foundation for production deployment. Without these migration scripts, the application cannot start in production mode with `ddl-auto: validate` because the schema won't exist. This blocks all deployments and prevents proper database version control.

**Independent Test**: Can be fully tested by starting a fresh database instance, running Flyway migrations, and verifying that the application starts successfully with `ddl-auto: validate` without any validation errors. Delivers immediate value by enabling production-ready database schema management.

**Acceptance Scenarios**:

1. **Given** a fresh MySQL database instance, **When** Flyway migrations are executed, **Then** all tables, indexes, and constraints for core, customer, product, order, invoice, unit, and form modules are created successfully
2. **Given** all Flyway migrations have been executed, **When** the application starts with `ddl-auto: validate`, **Then** no schema validation errors occur and the application starts normally
3. **Given** existing entities with relationships (foreign keys), **When** migrations are executed, **Then** all foreign key constraints are created in the correct order respecting dependencies
4. **Given** entities with indexes and unique constraints, **When** migrations are executed, **Then** all indexes and constraints match the JPA entity annotations

---

### User Story 2 - Establish Migration Versioning Strategy (Priority: P1)

As a development team, I need a clear and consistent versioning strategy for Flyway migrations so that multiple developers can work independently on database changes without version conflicts.

**Why this priority**: Without a standardized versioning approach, parallel development will lead to merge conflicts, migration ordering issues, and failed deployments. This must be established before any new features are developed.

**Independent Test**: Can be tested by having two developers create migrations simultaneously for different modules and verifying they can both be applied without conflicts. Delivers value by enabling parallel development and preventing deployment failures.

**Acceptance Scenarios**:

1. **Given** multiple modules (customer, product, order, invoice, unit, form), **When** migration scripts are created, **Then** each migration follows a consistent naming pattern with module-specific prefixes (e.g., `V4_1__create_customer_tables.sql`, `V4_2__create_product_tables.sql`)
2. **Given** existing migrations (V3_1 for tax, V3_2 for event), **When** new migrations are added, **Then** version numbers continue sequentially without gaps or duplicates
3. **Given** a multi-module migration requirement, **When** migration files are created, **Then** each file includes clear header comments documenting module, purpose, author, and dependencies
4. **Given** a new developer joining the project, **When** they read migration files, **Then** naming conventions and versioning patterns are self-explanatory from file structure and documentation

---

### User Story 3 - Document Migration Baseline and Conventions (Priority: P2)

As a developer onboarding to the project, I need clear documentation explaining migration conventions, baseline state, and how to create new migrations so that I can contribute database changes without breaking existing deployments.

**Why this priority**: This prevents mistakes and ensures consistency, but the system can function with P1 stories complete. Documentation improves developer productivity and reduces review cycles.

**Independent Test**: Can be tested by having a new developer follow the documentation to create a migration for a new entity and verify it follows all conventions without mentor review. Delivers value by reducing onboarding time and preventing common mistakes.

**Acceptance Scenarios**:

1. **Given** the migration directory, **When** a developer reviews README documentation, **Then** they understand the versioning scheme, naming conventions, and how to determine the next version number
2. **Given** a need to add a new table, **When** a developer follows migration templates, **Then** they create properly formatted SQL with consistent table structure, indexes, and constraints matching project standards
3. **Given** existing production databases, **When** reviewing baseline documentation, **Then** developers understand which migrations represent the baseline (existing entities) vs incremental changes (new features)

---

### Edge Cases

- What happens when a migration script is run against a database that already has some tables created manually or via `ddl-auto: update`?
- How does the system handle migration ordering when modules have cross-dependencies (e.g., product depends on unit)?
- What happens if a developer creates a migration with the same version number as another in-flight branch?
- How are data migrations handled separately from schema migrations?
- What happens when JPA entity annotations change but don't match the existing migration scripts?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide Flyway migration scripts for all existing JPA entities in core, customer, product, order, invoice, unit, and form modules
- **FR-002**: Migration scripts MUST create tables with column definitions matching JPA entity `@Column` annotations (name, type, length, nullable)
- **FR-003**: Migration scripts MUST create all indexes defined in JPA entity annotations (`@Index`, `@Table(indexes=...)`)
- **FR-004**: Migration scripts MUST create foreign key constraints for all JPA relationship annotations (`@ManyToOne`, `@OneToOne`, `@OneToMany`, `@ManyToMany`)
- **FR-005**: Migration scripts MUST respect entity inheritance hierarchies (`BaseDomain`, `OwnableBaseDomain`) and create appropriate columns for base classes
- **FR-006**: Migration version numbers MUST follow sequential ordering starting from V4_1 (after existing V3_1 tax and V3_2 event migrations)
- **FR-007**: Each migration script MUST include header comments documenting version, description, module, author, and date
- **FR-008**: Migration scripts MUST use MySQL-compatible SQL syntax matching the project's MySQL database configuration
- **FR-009**: Migration scripts MUST create tables in dependency order to avoid foreign key constraint errors during execution
- **FR-010**: System MUST validate that migration scripts are compatible with `ddl-auto: validate` mode by ensuring schema matches JPA entity definitions exactly
- **FR-011**: Migration file naming MUST follow pattern `V{major}_{minor}__{description}.sql` (e.g., `V4_1__create_customer_module_tables.sql`)
- **FR-012**: All timestamp columns MUST use `TIMESTAMP` type (not `DATETIME`) to maintain consistency with timezone UTC storage
- **FR-013**: Migration scripts MUST set appropriate table engine (`ENGINE=InnoDB`) and character set (`DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`)
- **FR-014**: Migration scripts MUST include unique constraints for columns annotated with `@Column(unique = true)` in JPA entities
- **FR-015**: System MUST provide a documentation file explaining migration versioning strategy, naming conventions, and baseline state

### Key Entities *(modules with entities requiring migrations)*

- **Customer Module**: Customer, CustomerGroup, CustomerType, CustomerImage, State, MasterState (6 entities)
- **Product Module**: Product, ProductImage, ProductPrice, Inventory, InventoryTransaction, InventoryUnitConversion (6+ entities)
- **Order Module**: Order, OrderItem (2 entities)
- **Invoice Module**: Invoice, InvoiceItem (2 entities)
- **Unit Module**: Unit, UnitConversion (2 entities)
- **Form Module**: AttributeDefinition, FieldConfig (2 entities)
- **Core Module**: File, Address, AbstractIdVerification (3 entities requiring migration review)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can start the application with a fresh database and have all required tables created automatically through Flyway migrations without manual SQL execution
- **SC-002**: Application starts successfully with `ddl-auto: validate` configuration against a database initialized only through Flyway migrations, with zero schema validation errors logged
- **SC-003**: Database schema creation time through Flyway migrations completes in under 30 seconds for all modules on standard development hardware
- **SC-004**: 100% of JPA entities have corresponding table creation statements in Flyway migration scripts, verified through automated entity-to-table mapping checks
- **SC-005**: Migration scripts execute successfully on fresh MySQL 8.0+ databases without foreign key constraint errors or ordering issues
- **SC-006**: Team members can independently create new migrations following documented conventions without requiring review cycles for basic syntax or versioning errors (95% first-submission acceptance rate)
- **SC-007**: Existing production-like databases can be baselined by marking initial migrations as completed, allowing future incremental migrations to execute without conflicts

## Dependencies *(mandatory)*

### Technical Dependencies

- **Flyway**: Project already includes Flyway dependency and configuration
- **MySQL 8.0+**: Database compatibility requirement for SQL syntax and features
- **JPA/Hibernate 6.2**: Entity definitions serve as source of truth for schema requirements
- **Spring Boot 3.5.6**: Configuration management for Flyway integration

### Module Dependencies

- **Migration Execution Order**: Core entities → Unit → Tax → Customer → Product → Order → Invoice → Form
- **Existing Migrations**: V3_1 (tax module) and V3_2 (event system) already exist and must not be modified
- **Multi-tenant Architecture**: All migrations must support `@TenantId` fields for workspace isolation (workspaceId columns)

## Assumptions *(mandatory)*

1. **Database State**: Production and staging databases may already have tables created through `ddl-auto: update` or manual scripts; these will require baseline migration marking
2. **Entity Stability**: Existing JPA entities are considered stable and their current structure represents the desired schema
3. **MySQL Database**: All environments use MySQL 8.0 or higher with UTC timezone configuration (`serverTimezone=UTC`)
4. **No Breaking Changes**: Migration scripts capture current entity state without modifying existing entity definitions
5. **Idempotency**: Migrations should be designed to fail fast if run against databases with existing tables (not using `IF NOT EXISTS`)
6. **Sequential Execution**: Flyway will execute migrations in version order; no parallel execution required
7. **Development Practice**: Developers will continue using `ddl-auto: update` in local development but migrations must match auto-generated schema
8. **Version Gaps**: Minor version numbers (4_1, 4_2, etc.) can have gaps to allow for emergency hotfix migrations if needed

## Constraints *(mandatory)*

### Technical Constraints

- **No Schema Modification**: Migration scripts must create schema matching existing entities without altering current JPA annotations
- **Backward Compatibility**: Migration scripts must work with existing V3_1 and V3_2 migrations already deployed
- **MySQL Syntax Only**: No cross-database compatibility required; optimize for MySQL syntax and features
- **File Location**: Migrations must live in the owning module under `src/main/resources/db/migration/mysql/`, ensuring Flyway discovers them via `classpath:db/migration/{vendor}`
- **No Data Migration**: Initial baseline migrations focus only on schema (DDL), not data (DML) operations

### Operational Constraints

- **Production Safety**: Migration scripts must be reversible or carefully tested; no automatic rollback supported by Flyway for DDL
- **Review Required**: All migration scripts require peer review before merging to prevent production schema issues
- **Testing Requirement**: Each migration must be tested against fresh MySQL database before commit
- **Version Control**: Migration files are immutable once merged; corrections require new versioned migrations

## Out of Scope *(mandatory)*

The following items are explicitly excluded from this feature:

1. **Data Migration**: Migrating existing data between table structures or transforming data formats
2. **Schema Refactoring**: Changing existing entity structures, renaming columns, or normalizing tables
3. **Database Seeding**: Creating default/seed data for master tables (handled separately by application seeders)
4. **Cross-Database Support**: PostgreSQL, Oracle, or other database compatibility (MySQL only)
5. **Migration Rollback Scripts**: Undo/down migrations for reverting changes (Flyway OSS doesn't support down migrations)
6. **Automated Entity Scanning**: Tools to auto-generate migrations from JPA entities (manual creation with validation)
7. **Performance Optimization**: Adding additional indexes or optimizing existing queries beyond entity definitions
8. **Audit Trail Tables**: Creating separate audit or history tracking tables (unless explicitly defined in entities)
9. **Business Module Migration**: The business module already has its own migrations in `ampairs-backend/business/src/main/resources/db/migration/`
10. **Workspace Module Migration**: The workspace module already has migrations managed separately

## Risks *(mandatory)*

### High Priority Risks

1. **Risk**: Existing production databases have schema drift (manual changes not in entities)
   - **Mitigation**: Generate migrations, compare with production schema using Flyway validate, document differences
   - **Impact**: Could cause application startup failures with `ddl-auto: validate`

2. **Risk**: Migration execution fails due to incorrect foreign key dependency ordering
   - **Mitigation**: Manually review and test migration sequence, use topological sort for module dependencies
   - **Impact**: Flyway execution fails, requires manual intervention to reorder migrations

3. **Risk**: JPA entity annotations don't fully represent required schema (missing indexes, constraints)
   - **Mitigation**: Compare auto-generated schema from `ddl-auto: update` with entity annotations, add missing definitions
   - **Impact**: Application may work but with degraded performance or missing data integrity constraints

### Medium Priority Risks

4. **Risk**: Multiple developers create migrations with same version number on parallel branches
   - **Mitigation**: Document versioning strategy, reserve version ranges per feature branch, use branch-specific minor versions
   - **Impact**: Merge conflicts, need to renumber migrations before merge

5. **Risk**: Migration scripts include MySQL-specific syntax not compatible with future database migrations
   - **Mitigation**: Document MySQL requirement clearly, use standard SQL where possible within MySQL dialect
   - **Impact**: Future database platform changes require migration script rewrites
