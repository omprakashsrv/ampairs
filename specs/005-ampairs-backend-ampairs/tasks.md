# Tasks: Database Schema Migration with Flyway

**Feature Branch**: `005-ampairs-backend-ampairs`
**Input**: Design documents from `/specs/005-ampairs-backend-ampairs/`
**Prerequisites**: plan.md , spec.md , research.md , data-model.md , contracts/ , quickstart.md 

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions
- Migration directory: `{module}/src/main/resources/db/migration/mysql/` (e.g., `ampairs-backend/customer/...`)
- Test directory: `ampairs-backend/ampairs_service/src/test/kotlin/com/ampairs/`
- Documentation: `specs/005-ampairs-backend-ampairs/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare environment and verify existing Flyway configuration

- [x] **T001** [Setup] Verify Flyway configuration in `ampairs-backend/ampairs_service/src/main/resources/application.yml` (ddl-auto: validate, flyway.enabled: true, baseline-on-migrate: true)
- [ ] **T002** [P] [Setup] Start MySQL test container and verify connectivity (Docker required)
- [ ] **T003** [P] [Setup] Backup existing database schema documentation (if any)

**Estimated Time**: 30 minutes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Research entity structure and establish baseline understanding

** CRITICAL**: No migration creation can begin until entity analysis is complete

- [x] **T004** [Foundation] Analyze all JPA entities in `ampairs-backend/core/src/main/kotlin/com/ampairs/core/domain/model/` (BaseDomain, OwnableBaseDomain, File, Address)
- [x] **T005** [P] [Foundation] Analyze all JPA entities in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/model/` (Unit, UnitConversion)
- [x] **T006** [P] [Foundation] Analyze all JPA entities in `ampairs-backend/customer/src/main/kotlin/com/ampairs/customer/domain/model/` (Customer, CustomerGroup, CustomerType, CustomerImage, State, MasterState)
- [x] **T007** [P] [Foundation] Analyze all JPA entities in `ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/model/` and `ampairs-backend/product/src/main/kotlin/com/ampairs/inventory/domain/model/` (Product, ProductGroup, ProductBrand, ProductCategory, ProductSubCategory, ProductImage, ProductPrice, Inventory, InventoryTransaction, InventoryUnitConversion)
- [x] **T008** [P] [Foundation] Analyze all JPA entities in `ampairs-backend/order/src/main/kotlin/com/ampairs/order/domain/model/` (Order, OrderItem)
- [x] **T009** [P] [Foundation] Analyze all JPA entities in `ampairs-backend/invoice/src/main/kotlin/com/ampairs/invoice/domain/model/` (Invoice, InvoiceItem)
- [x] **T010** [P] [Foundation] Analyze all JPA entities in `ampairs-backend/form/src/main/kotlin/com/ampairs/form/domain/model/` (AttributeDefinition, FieldConfig)
- [x] **T011** [Foundation] Create entity dependency graph to determine migration order (core → unit → customer → product → order → invoice → form)

**Checkpoint**: Entity analysis complete - migration creation can now begin in parallel by module

**Estimated Time**: 2-3 hours

---

## Phase 3: User Story 1 - Generate Missing Flyway Migration Scripts (Priority: P1) < MVP

**Goal**: Create comprehensive Flyway migration scripts for all 22+ existing JPA entities to enable production deployment with `ddl-auto: validate`

**Independent Test**: Start fresh MySQL database, run `./gradlew flywayMigrate`, start application with `ddl-auto: validate`, verify no schema validation errors

### Migration V4_1: Core Module

- [x] **T012** [US1] Create migration file `ampairs-backend/core/src/main/resources/db/migration/mysql/V1.0.0__create_core_tables.sql`
- [x] **T013** [US1] Add migration header with version 4.1, description "Create core module tables", author, date, dependencies
- [x] **T014** [US1] Add `file` table DDL matching File.kt entity (storage_url, file_name, file_type, file_size, uploaded_by) with BaseDomain fields
- [x] **T015** [US1] Add `address` table DDL if Address.kt is standalone entity (check if embedded type or standalone table) — Address is value object, documented as no-op
- [x] **T016** [US1] Add indexes for file table (idx_file_uid unique, idx_file_uploaded_by) — added uid/owner/bucket indexes; uploaded_by column not present in entity
- [ ] **T017** [US1] Verify V4_1 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_2: Unit Module

- [x] **T018** [P] [US1] Create migration file `ampairs-backend/unit/src/main/resources/db/migration/mysql/V4_2__create_unit_module_tables.sql`
- [x] **T019** [US1] Add migration header with version 4.2, description "Create unit and unit_conversion tables", author, date, dependencies (requires V4_1)
- [x] **T020** [US1] Add `unit` table DDL matching Unit.kt entity (name VARCHAR(10), short_name VARCHAR(10), decimal_places INT, active BOOLEAN) with OwnableBaseDomain fields (owner_id, ref_id)
- [x] **T021** [US1] Add indexes for unit table (unit_idx on name, idx_unit_uid unique)
- [x] **T022** [US1] Add `unit_conversion` table DDL matching UnitConversion.kt entity (base_unit_id, derived_unit_id, multiplier DOUBLE, active BOOLEAN) with OwnableBaseDomain fields
- [x] **T023** [US1] Add foreign keys for unit_conversion (base_unit_id  unit.uid ON DELETE CASCADE, derived_unit_id  unit.uid ON DELETE CASCADE)
- [x] **T024** [US1] Add indexes for unit_conversion (idx_unit_conversion_base, idx_unit_conversion_derived)
- [ ] **T025** [US1] Verify V4_2 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_3: Customer Module

- [x] **T026** [P] [US1] Create migration file `ampairs-backend/customer/src/main/resources/db/migration/mysql/V1.0.0__create_customer_module_tables.sql`
- [x] **T027** [US1] Add migration header with version 4.3, description "Create customer module tables", author, date, dependencies (requires V4_1)
- [x] **T028** [US1] Add `customer_group` table DDL matching CustomerGroup.kt (name, description TEXT) with OwnableBaseDomain
- [x] **T029** [US1] Add `customer_type` table DDL matching CustomerType.kt (name, description TEXT) with OwnableBaseDomain
- [x] **T030** [US1] Add `master_state` table DDL matching MasterState.kt (name, state_code, country) with OwnableBaseDomain — implemented as BaseDomain per entity
- [x] **T031** [US1] Add `customer` table DDL matching Customer.kt (country_code INT, name, customer_type, customer_group, phone, status, landline, email, gst_number VARCHAR(15), pan_number VARCHAR(10), credit_limit DOUBLE, credit_days INT, outstanding_amount DOUBLE, address, street, street2, city, pincode, state, country, location POINT, billing_address JSON, shipping_address JSON) with OwnableBaseDomain
- [x] **T032** [US1] Add indexes for customer (idx_customer_name, idx_customer_phone, idx_customer_email, uk_customer_gst unique on gst_number if not null)
- [x] **T033** [US1] Add `customer_image` table DDL matching CustomerImage.kt (customer_uid, storage_url) with OwnableBaseDomain
- [x] **T034** [US1] Add foreign key for customer_image (customer_uid  customer.uid ON DELETE CASCADE)
- [ ] **T035** [US1] Verify V4_3 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_4: Product Module

- [x] **T036** [P] [US1] Create migration file `ampairs-backend/product/src/main/resources/db/migration/mysql/V4_4__create_product_module_tables.sql`
- [x] **T037** [US1] Add migration header with version 4.4, description "Create product and inventory module tables", author, date, dependencies (requires V4_2 unit module)
- [x] **T038** [US1] Add `product_group` table DDL matching ProductGroup.kt (name) with OwnableBaseDomain
- [x] **T039** [US1] Add `product_brand` table DDL matching ProductBrand.kt (name) with OwnableBaseDomain
- [x] **T040** [US1] Add `product_category` table DDL matching ProductCategory.kt (name) with OwnableBaseDomain
- [x] **T041** [US1] Add `product_sub_category` table DDL matching ProductSubCategory.kt (name, category_id) with OwnableBaseDomain — entity lacks category link; documented in migration comments
- [x] **T042** [US1] Add foreign key for product_sub_category (category_id  product_category.uid ON DELETE CASCADE) — not applicable because entity has no category reference; captured via comment
- [x] **T043** [US1] Add `product` table DDL matching Product.kt (name, code, sku VARCHAR(100) UNIQUE, description TEXT, status, tax_code_id, tax_code, unit_id, base_price DOUBLE, cost_price DOUBLE, group_id, brand_id, category_id, sub_category_id, base_unit_id, mrp DOUBLE, dp DOUBLE, selling_price DOUBLE, index_no INT, attributes JSON) with OwnableBaseDomain
- [x] **T044** [US1] Add indexes for product (idx_product_uid unique, uk_product_sku unique, idx_product_name)
- [x] **T045** [US1] Add optional foreign keys for product (unit_id  unit.uid ON DELETE SET NULL if needed)
- [x] **T046** [US1] Add `product_image` table DDL matching ProductImage.kt (product_id, storage_url) with OwnableBaseDomain
- [x] **T047** [US1] Add foreign key for product_image (product_id  product.uid ON DELETE CASCADE) — FK omitted due to column length mismatch; rationale noted in migration
- [x] **T048** [US1] Add `product_price` table DDL matching ProductPrice.kt entity (analyze entity first for fields)
- [x] **T049** [US1] Add `inventory` table DDL matching Inventory.kt (description, product_id, stock DOUBLE, selling_price DOUBLE, buying_price DOUBLE, mrp DOUBLE, dp DOUBLE, unit_id) with OwnableBaseDomain
- [x] **T050** [US1] Add foreign keys for inventory (product_id  product.uid ON DELETE CASCADE, unit_id  unit.uid ON DELETE SET NULL) — product FK omitted for length mismatch; unit FK applied
- [x] **T051** [US1] Add `inventory_transaction` table DDL matching InventoryTransaction.kt (description, product_id, stock DOUBLE, selling_price DOUBLE, mrp DOUBLE, dp DOUBLE, unit_id) with OwnableBaseDomain
- [x] **T052** [US1] Add foreign keys for inventory_transaction (product_id  product.uid, unit_id  unit.uid ON DELETE SET NULL) — product FK omitted for length mismatch; unit FK applied
- [x] **T053** [US1] Add `inventory_unit_conversion` table DDL matching InventoryUnitConversion.kt (base_unit_id, derived_unit_id, inventory_id, multiplier DOUBLE) with OwnableBaseDomain
- [x] **T054** [US1] Add foreign keys for inventory_unit_conversion (base_unit_id  unit.uid, derived_unit_id  unit.uid, inventory_id  inventory.uid ON DELETE CASCADE)
- [ ] **T055** [US1] Verify V4_4 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_5: Order Module

- [x] **T056** [P] [US1] Create migration file `ampairs-backend/order/src/main/resources/db/migration/mysql/V4_5__create_order_module_tables.sql`
- [x] **T057** [US1] Add migration header with version 4.5, description "Create order module tables", author, date, dependencies (requires V4_3 customer, V4_4 product)
- [x] **T058** [US1] Add `customer_order` table DDL matching Order.kt (order_number, order_type, customer_id, customer_name, customer_phone, is_walk_in BOOLEAN, payment_method, invoice_ref_id, order_date TIMESTAMP, delivery_date TIMESTAMP, from_customer_id, from_customer_name, to_customer_id, to_customer_name, place_of_supply, from_customer_gst, to_customer_gst, subtotal DOUBLE, discount_amount DOUBLE, tax_amount DOUBLE, total_amount DOUBLE, total_cost DOUBLE, base_price DOUBLE, total_tax DOUBLE, notes TEXT, status VARCHAR(20)) with OwnableBaseDomain
- [x] **T059** [US1] Add indexes for customer_order (idx_order_uid unique, order_ref_idx unique on ref_id, idx_order_customer on customer_id)
- [x] **T060** [US1] Add optional foreign key for customer_order (customer_id  customer.uid ON DELETE RESTRICT if enforced) — FK omitted because customer_id length differs from customer.uid; limitation noted
- [x] **T061** [US1] Add `order_item` table DDL matching OrderItem.kt (analyze entity for fields: order_id, product_id, quantity DOUBLE, unit_price DOUBLE, total_price DOUBLE) with OwnableBaseDomain
- [x] **T062** [US1] Add foreign keys for order_item (order_id  customer_order.uid ON DELETE CASCADE, product_id  product.uid ON DELETE RESTRICT) — documented length mismatch prevents enforcement; noted in migration comments
- [ ] **T063** [US1] Verify V4_5 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_6: Invoice Module

- [x] **T064** [P] [US1] Create migration file `ampairs-backend/invoice/src/main/resources/db/migration/mysql/V1.0.0__create_invoice_module_tables.sql`
- [x] **T065** [US1] Add migration header with version 4.6, description "Create invoice module tables", author, date, dependencies (requires V4_3 customer, V4_4 product, V4_5 order)
- [x] **T066** [US1] Add `invoice` table DDL matching Invoice.kt (analyze entity for fields: invoice_number, order_ref_id, from_customer_id, to_customer_id, invoice_date TIMESTAMP, due_date TIMESTAMP, subtotal DOUBLE, tax_amount DOUBLE, total_amount DOUBLE, paid_amount DOUBLE, status, notes TEXT) with OwnableBaseDomain
- [x] **T067** [US1] Add indexes for invoice (uk_invoice_number unique, idx_invoice_customer on to_customer_id)
- [x] **T068** [US1] Add optional foreign keys for invoice (order_ref_id  customer_order.ref_id ON DELETE SET NULL, to_customer_id  customer.uid ON DELETE RESTRICT) — order_ref FK implemented; customer FK omitted due to length mismatch and documented
- [x] **T069** [US1] Add `invoice_item` table DDL matching InvoiceItem.kt (invoice_id, product_id, quantity DOUBLE, unit_price DOUBLE, total_price DOUBLE) with OwnableBaseDomain
- [x] **T070** [US1] Add foreign keys for invoice_item (invoice_id  invoice.uid ON DELETE CASCADE, product_id  product.uid ON DELETE RESTRICT) — FK enforcement skipped for column length mismatch; recorded in migration comments
- [ ] **T071** [US1] Verify V4_6 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_7: Form Module (Optional)

- [x] **T072** [P] [US1] Create migration file `ampairs-backend/form/src/main/resources/db/migration/mysql/V1.0.0__create_form_module_tables.sql` (if needed)
- [x] **T073** [US1] Add migration header with version 4.7, description "Create form module tables", author, date, dependencies (requires V4_1)
- [x] **T074** [US1] Add `attribute_definition` table DDL matching AttributeDefinition.kt (analyze entity for fields) with OwnableBaseDomain
- [x] **T075** [US1] Add `field_config` table DDL matching FieldConfig.kt (analyze entity for fields: field_name, field_type, validation_rules JSON) with OwnableBaseDomain
- [ ] **T076** [US1] Verify V4_7 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_8: Auth Module

- [x] **T076a** [P] [US1] Create migration file `ampairs-backend/auth/src/main/resources/db/migration/mysql/V1.0.1__create_auth_module_tables.sql`
- [x] **T076b** [US1] Create PostgreSQL equivalent `V1.0.1__create_auth_module_tables.sql` under `db/migration/postgresql/`
- [x] **T076c** [US1] Add `device_session`, `login_session`, and `auth_token` table DDL matching auth entities with BaseDomain fields and required indexes
- [ ] **T076d** [US1] Verify V4_8 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_9: Workspace Module

- [x] **T076e** [P] [US1] Create migration file `ampairs-backend/workspace/src/main/resources/db/migration/mysql/V4_9__create_workspace_module_tables.sql`
- [x] **T076f** [US1] Create PostgreSQL equivalent `V4_9__create_workspace_module_tables.sql`
- [x] **T076g** [US1] Add workspace table DDLs (workspaces, workspace_members, workspace_invitations, workspace_teams, master_modules, workspace_modules, workspace_settings, workspace_activities)
- [ ] **T076h** [US1] Verify V4_9 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Migration V4_10: Notification Module

- [x] **T076i** [P] [US1] Create migration file `ampairs-backend/notification/src/main/resources/db/migration/mysql/V4_10__create_notification_module_tables.sql`
- [x] **T076j** [US1] Create PostgreSQL equivalent `V4_10__create_notification_module_tables.sql`
- [x] **T076k** [US1] Add `notification_queue` table DDL matching NotificationQueue entity (status/retry indices, JSON/text columns)
- [ ] **T076l** [US1] Verify V4_10 migration syntax with `./gradlew :ampairs_service:flywayValidate`

### Integration Testing for User Story 1

- [x] **T077** [US1] Create `ampairs-backend/ampairs_service/src/test/kotlin/com/ampairs/FlywayMigrationTest.kt` with Testcontainers setup (MySQLContainer)
- [x] **T078** [US1] Add test: `should successfully execute all migrations` - verify Flyway runs migrations V4_1 through V4_10 on fresh MySQL 8.0 container
- [x] **T079** [US1] Add test: `should validate JPA entities match database schema` - verify ddl-auto:validate passes with EntityManager metamodel checks
- [x] **T080** [US1] Add test: `should verify foreign key constraints exist` - query information_schema.KEY_COLUMN_USAGE to verify all FK relationships
- [x] **T081** [US1] Add test: `should verify indexes exist on foreign keys` - query information_schema.STATISTICS to check indexes
- [x] **T082** [US1] Add test: `should verify JSON columns are properly typed` - query information_schema.COLUMNS for DATA_TYPE = 'json'
- [x] **T083** [US1] Add test: `should verify timestamp columns use TIMESTAMP type` - check created_at, updated_at columns are TIMESTAMP not DATETIME
- [ ] **T084** [US1] Run test suite with `./gradlew :ampairs_service:test --tests FlywayMigrationTest` and verify all tests pass

### Verification and Cleanup for User Story 1

- [ ] **T085** [US1] Start fresh MySQL test database with Docker: `docker run -d --name flyway-test -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=test_db -p 3307:3306 mysql:8.0`
- [ ] **T086** [US1] Run Flyway migrations: `./gradlew :ampairs_service:flywayMigrate -Dspring.datasource.url=jdbc:mysql://localhost:3307/test_db?serverTimezone=UTC`
- [ ] **T087** [US1] Verify migration history: `docker exec flyway-test mysql -uroot -proot test_db -e "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"`
- [ ] **T088** [US1] Verify all tables created: `docker exec flyway-test mysql -uroot -proot test_db -e "SHOW TABLES;"`
- [ ] **T089** [US1] Start application with ddl-auto:validate: `./gradlew :ampairs_service:bootRun` with test profile and verify no schema validation errors in logs
- [ ] **T090** [US1] Test CRUD operations for each module (unit, customer, product, order, invoice) via REST API or direct repository access
- [ ] **T091** [US1] Stop and remove test container: `docker stop flyway-test && docker rm flyway-test`

**Checkpoint**: At this point, User Story 1 is fully functional - all migrations created, tested, and validated against JPA entities

**Estimated Time**: 12-16 hours (careful entity analysis, SQL writing, testing)

---

## Phase 4: User Story 2 - Establish Migration Versioning Strategy (Priority: P1)

**Goal**: Document and enforce consistent migration versioning and naming conventions to prevent conflicts

**Independent Test**: Two developers create migrations simultaneously (e.g., V4_8 and V4_9) and verify both can be applied without conflicts

### Documentation for User Story 2

- [x] **T092** [US2] Create `ampairs-backend/ampairs_service/src/main/resources/db/migration/README.md` documenting migration conventions
- [x] **T093** [US2] Add section "Versioning Scheme" explaining V{major}_{minor}__description.sql pattern (e.g., V4_1, V4_2)
- [x] **T094** [US2] Add section "Naming Conventions" with examples: V4_1__create_unit_module_tables.sql (lowercase with underscores)
- [x] **T095** [US2] Add section "Header Template" showing required comments (version, description, author, date, dependencies, tables created)
- [x] **T096** [US2] Add section "Version Number Assignment" explaining how to determine next version (check existing migrations, reserve ranges for features)
- [x] **T097** [US2] Add section "Module Organization" explaining one migration per module approach (V4_1 core, V4_2 unit, V4_3 customer, etc.)
- [x] **T098** [US2] Add section "Dependency Documentation" showing how to document cross-module dependencies in migration headers
- [x] **T099** [US2] Add section "Parallel Development" explaining version range reservation strategy (e.g., feature A reserves V4_10-V4_15, feature B reserves V4_16-V4_20)
- [x] **T100** [US2] Add examples of correct and incorrect migration file names with explanations
- [x] **T101** [US2] Add troubleshooting section for common versioning issues (duplicate versions, out-of-order execution, checksum mismatches)

### Validation Rules for User Story 2

- [x] **T102** [P] [US2] Document validation checklist in README.md: syntax validation, header completeness, dependency order, naming pattern compliance
- [x] **T103** [P] [US2] Add example Gradle task or script to validate migration naming pattern matches V{n}_{m}__{description}.sql regex
- [x] **T104** [US2] Document how to use `./gradlew flywayInfo` to check migration status and order
- [x] **T105** [US2] Document how to use `./gradlew flywayValidate` to verify checksums and detect modified migrations
- [x] **T106** [US2] Add git commit message template for migration commits: "feat(db): add migration V4_X - {description}"

### Review Process for User Story 2

- [x] **T107** [US2] Document migration review checklist: SQL syntax correctness, column types match JPA entities, indexes match annotations, foreign keys follow conventions, CASCADE rules appropriate
- [x] **T108** [US2] Add section "Common Review Findings" with examples: DATETIME vs TIMESTAMP, missing indexes on foreign keys, incorrect VARCHAR lengths, missing NOT NULL constraints
- [x] **T109** [US2] Document rollback procedures: manual DROP TABLE statements in reverse order, no automatic rollback in Flyway Community Edition
- [x] **T110** [US2] Add section "Emergency Hotfix Migrations" explaining how to insert migrations between existing ones if needed (e.g., V4_2_1 between V4_2 and V4_3)

**Checkpoint**: At this point, User Story 2 is complete - clear versioning strategy documented and enforceable

**Estimated Time**: 3-4 hours

---

## Phase 5: User Story 3 - Document Migration Baseline and Conventions (Priority: P2)

**Goal**: Create comprehensive onboarding documentation for developers to understand migration workflow and conventions

**Independent Test**: New developer follows documentation to create migration for hypothetical new entity and gets approval without mentor intervention

### Baseline Documentation for User Story 3

- [x] **T111** [US3] Create `ampairs-backend/ampairs_service/src/main/resources/db/migration/MIGRATION_BASELINE.md` documenting baseline strategy
- [x] **T112** [US3] Add section "What is Baseline?" explaining Flyway baseline concept and when it's needed (existing databases with tables)
- [x] **T113** [US3] Add section "Current Baseline State" documenting which migrations (V4_1-V4_7) represent baseline for existing entities
- [x] **T114** [US3] Add section "Fresh Database Setup" with step-by-step: clone repo  docker-compose up  ./gradlew flywayMigrate  verify tables
- [x] **T115** [US3] Add section "Existing Database Baseline" documenting how baseline-on-migrate: true works automatically (creates baseline at V0, executes V4_1+)
- [x] **T116** [US3] Add section "Production Deployment" with pre-deployment checklist: backup database, test on staging, verify ddl-auto:validate, monitor logs
- [x] **T117** [US3] Add section "Rollback Procedures" with manual SQL scripts to drop tables in reverse dependency order
- [x] **T118** [US3] Document environment-specific strategies: development (flywayClean allowed), staging (baseline testing), production (baseline-on-migrate)

### Developer Onboarding Guide for User Story 3

- [x] **T119** [US3] Create `specs/005-ampairs-backend-ampairs/DEVELOPER_GUIDE.md` (or enhance existing quickstart.md)
- [x] **T120** [US3] Add section "Getting Started" with prerequisites: Docker, MySQL client, understanding of JPA entities
- [x] **T121** [US3] Add section "Creating Your First Migration" with step-by-step walkthrough using example entity
- [x] **T122** [US3] Add section "JPA to SQL Mapping Cheat Sheet" with common type mappings (String  VARCHAR, Instant  TIMESTAMP, JSON, Point)
- [x] **T123** [US3] Add section "Testing Migrations Locally" with Docker commands and Gradle tasks
- [x] **T124** [US3] Add section "Common Pitfalls" with solutions: foreign key ordering, TIMESTAMP vs DATETIME, JSON column types, VARCHAR length mismatches
- [x] **T125** [US3] Add section "Debugging Failed Migrations" with troubleshooting steps: check logs, query flyway_schema_history, use flywayRepair
- [x] **T126** [US3] Add section "IDE Setup Tips" for IntelliJ IDEA or VS Code with SQL syntax highlighting and validation

### Migration Templates for User Story 3

- [x] **T127** [P] [US3] Enhance `specs/005-ampairs-backend-ampairs/contracts/migration-template.sql` with more inline examples
- [x] **T128** [P] [US3] Add template section for entities with JSON columns showing @JdbcTypeCode(SqlTypes.JSON)  JSON mapping
- [x] **T129** [P] [US3] Add template section for entities with spatial types showing Point  POINT mapping
- [x] **T130** [P] [US3] Add template section for self-referential foreign keys (like unit_conversion referencing unit twice)
- [x] **T131** [US3] Create example migration file `example_V4_8__create_example_entity.sql` with full annotations showing every pattern
- [x] **T132** [US3] Document when to use CASCADE vs RESTRICT vs SET NULL for foreign keys with decision tree

### Quick Reference Documentation for User Story 3

- [x] **T133** [P] [US3] Create cheat sheet file `FLYWAY_CHEATSHEET.md` with common commands: flywayInfo, flywayMigrate, flywayValidate, flywayBaseline, flywayRepair
- [x] **T134** [P] [US3] Add SQL cheat sheet section with common queries: check migration history, list tables, describe table structure, show foreign keys, show indexes
- [x] **T135** [P] [US3] Add troubleshooting matrix mapping error messages to solutions (e.g., "cannot add foreign key"  check parent table exists and column types match)
- [x] **T136** [US3] Document test-driven workflow: write migration  run flywayValidate  run tests  start app with ddl-auto:validate  verify CRUD operations

**Checkpoint**: At this point, User Story 3 is complete - comprehensive documentation enables independent developer onboarding

**Estimated Time**: 4-6 hours

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, cleanup, and production readiness

- [x] **T137** [Polish] Review all migration files for consistent formatting (2-space indentation, aligned columns, clear comments)
- [x] **T138** [P] [Polish] Review all migration headers for completeness (version, description, author, date, dependencies, tables listed)
- [x] **T139** [P] [Polish] Verify all foreign key CASCADE rules are appropriate (@OneToMany  CASCADE, @ManyToOne  RESTRICT, @OneToOne nullable  SET NULL)
- [x] **T140** [Polish] Update main `CLAUDE.md` file to document new migration baseline and conventions (if not already updated by agent context script)
- [x] **T141** [Polish] Create `specs/005-ampairs-backend-ampairs/DEPLOYMENT_CHECKLIST.md` with production deployment steps
- [x] **T142** [Polish] Add staging deployment verification procedure: backup staging DB  deploy  run smoke tests  verify flyway_schema_history
- [x] **T143** [P] [Polish] Add production deployment verification procedure: backup production DB  enable maintenance mode  deploy  verify migrations  smoke tests  disable maintenance mode
- [x] **T144** [Polish] Document monitoring and alerting for migration failures (application startup logs, flyway_schema_history queries, schema validation errors)
- [x] **T145** [Polish] Create rollback runbook with manual SQL scripts and decision tree for when to rollback vs fix forward
- [ ] **T146** [Polish] Final review of all tasks and mark any optional tasks clearly (e.g., V4_7 form module if not immediately needed)

**Checkpoint**: Feature complete and production-ready

**Estimated Time**: 2-3 hours

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)
**User Story 1 (P1)** alone constitutes the MVP:
- Create all migration files (V4_1 through V4_6, optionally V4_7)
- Test migrations on fresh database
- Verify application starts with ddl-auto:validate
- Enable production deployment

**Time Estimate**: 12-16 hours

### Full Feature Delivery
**User Stories 1 + 2 (Both P1)**:
- MVP migrations + versioning strategy documentation
- Enables team collaboration and parallel development

**Time Estimate**: 15-20 hours

### Complete Feature
**All User Stories (P1 + P2)**:
- Migrations + versioning + comprehensive onboarding documentation
- Optimal developer experience and reduced onboarding time

**Time Estimate**: 20-26 hours

---

## Dependencies and Parallelization

### Story Dependencies
```
Setup (Phase 1)  Foundation (Phase 2)  , US1 (P1)  US2 (P1)  US3 (P2)  Polish
                                           Can work independently once foundation complete
```

**Key Insight**: US1, US2, US3 have no dependencies on each other. US1 creates migrations, US2 documents versioning, US3 creates onboarding guides. All three can be worked on in parallel after Phase 2.

### Parallel Execution Opportunities

**Phase 1 (Setup)**: T002-T003 can run in parallel

**Phase 2 (Foundation)**: T005-T010 can run in parallel (entity analysis by module)

**Phase 3 (US1 - Migration Creation)**: High parallelization:
- T012-T017 (V4_1 core) [P]
- T018-T025 (V4_2 unit) [P]
- T026-T035 (V4_3 customer) [P]
- T036-T055 (V4_4 product) [P]
- T056-T063 (V4_5 order) [P]
- T064-T071 (V4_6 invoice) [P]
- T072-T076 (V4_7 form) [P]

All 7 migration files can be created in parallel by different developers or AI agents.

**Phase 4 (US2)**: T102-T106 can run in parallel (different documentation sections)

**Phase 5 (US3)**: T127-T132 can run in parallel (templates), T133-T136 can run in parallel (cheat sheets)

**Phase 6 (Polish)**: T138-T139, T142-T143 can run in parallel

### Maximum Parallelization Example
With 7 developers/agents:
1. Agent 1: V4_1 core (T012-T017)
2. Agent 2: V4_2 unit (T018-T025)
3. Agent 3: V4_3 customer (T026-T035)
4. Agent 4: V4_4 product (T036-T055)
5. Agent 5: V4_5 order (T056-T063)
6. Agent 6: V4_6 invoice (T064-T071)
7. Agent 7: V4_7 form (T072-T076)

**Result**: All migrations created in parallel (4-6 hours vs 12-16 hours sequential)

---

## Task Summary

**Total Tasks**: 146
- Phase 1 (Setup): 3 tasks
- Phase 2 (Foundation): 8 tasks
- Phase 3 (US1 - MVP): 80 tasks
- Phase 4 (US2): 19 tasks
- Phase 5 (US3): 26 tasks
- Phase 6 (Polish): 10 tasks

**User Story Breakdown**:
- US1 (P1 - Generate Migrations): 80 tasks (55% of total)
- US2 (P1 - Versioning Strategy): 19 tasks (13% of total)
- US3 (P2 - Documentation): 26 tasks (18% of total)
- Setup + Foundation + Polish: 21 tasks (14% of total)

**Parallel Opportunities**: 45+ tasks marked [P] for parallelization

**Critical Path**: Phase 1  Phase 2  US1 core migration creation  Testing (T077-T091)  Verification

**Suggested First Sprint**: Setup + Foundation + US1 (T001-T091) = MVP delivery in 12-16 hours
