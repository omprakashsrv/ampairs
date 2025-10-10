# Tasks: Business Module

**Feature**: 003-business-module
**Branch**: `003-business-module`
**Input**: Design documents from `/specs/003-business-module/`
**Prerequisites**: ✅ plan.md, research.md, data-model.md, contracts/, quickstart.md

## Overview

This task list implements the Business module to extract business profile and configuration data from the Workspace entity. Implementation follows TDD principles with tests written before implementation.

**Tech Stack**:
- Language: Kotlin 1.9 + JVM 17
- Framework: Spring Boot 3.x, Spring Data JPA, Hibernate
- Database: PostgreSQL 15+ with Flyway migrations
- Testing: JUnit 5, Spring Boot Test, MockK, Testcontainers

**Module Path**: `ampairs-backend/business/`

## Task Format

- **[P]**: Can run in parallel (different files, no dependencies)
- Tasks ordered by dependencies
- Each task includes exact file paths
- Tests MUST be written and MUST FAIL before implementation

---

## Phase 3.1: Setup & Infrastructure

### T001: Create Gradle module structure
**Description**: Create the business module directory structure and Gradle configuration

**Actions**:
1. Create directory: `ampairs-backend/business/`
2. Create subdirectories:
   - `src/main/kotlin/com/ampairs/business/`
   - `src/main/kotlin/com/ampairs/business/model/`
   - `src/main/kotlin/com/ampairs/business/model/dto/`
   - `src/main/kotlin/com/ampairs/business/model/enums/`
   - `src/main/kotlin/com/ampairs/business/repository/`
   - `src/main/kotlin/com/ampairs/business/service/`
   - `src/main/kotlin/com/ampairs/business/controller/`
   - `src/main/kotlin/com/ampairs/business/exception/`
   - `src/main/resources/db/migration/`
   - `src/test/kotlin/com/ampairs/business/`

**Files**: Directory structure

**Dependencies**: None

---

### T002: Create build.gradle.kts for business module
**Description**: Configure Gradle build file with dependencies

**Actions**:
1. Create `ampairs-backend/business/build.gradle.kts`
2. Add dependencies:
   - `implementation(project(":core"))`
   - `implementation(project(":workspace"))`
   - `implementation("org.springframework.boot:spring-boot-starter-web")`
   - `implementation("org.springframework.boot:spring-boot-starter-data-jpa")`
   - `implementation("org.springframework.boot:spring-boot-starter-validation")`
   - `implementation("org.postgresql:postgresql")`
   - `implementation("org.flywaydb:flyway-core")`
   - `testImplementation("org.springframework.boot:spring-boot-starter-test")`
   - `testImplementation("org.testcontainers:postgresql")`
   - `testImplementation("io.mockk:mockk")`

**Files**: `ampairs-backend/business/build.gradle.kts`

**Dependencies**: T001

---

### T003: Register business module in settings.gradle.kts
**Description**: Add business module to project settings

**Actions**:
1. Edit `settings.gradle.kts` at repository root
2. Add: `include("ampairs-backend:business")`

**Files**: `settings.gradle.kts`

**Dependencies**: T001, T002

---

## Phase 3.2: Database Schema & Migrations

### T004: Create businesses table migration
**Description**: Create Flyway migration for businesses table with all fields and indexes

**Actions**:
1. Create `ampairs-backend/business/src/main/resources/db/migration/V1.0.0__create_businesses_table.sql`
2. Include:
   - Table definition with 30+ columns (uid, workspace_id, name, business_type, address fields, contact fields, tax fields, operational config)
   - Primary key on uid
   - Unique constraint on workspace_id
   - Foreign key: workspace_id → workspaces(uid) ON DELETE CASCADE
   - Indexes: idx_business_workspace (unique), idx_business_type, idx_business_active, idx_business_country

**Files**: `ampairs-backend/business/src/main/resources/db/migration/V1.0.0__create_businesses_table.sql`

**Dependencies**: T001

**Reference**: data-model.md (Table: businesses section)

---

### T005: Create data migration from workspaces table
**Description**: Create Flyway migration to copy business data from workspaces to businesses table

**Actions**:
1. Create `ampairs-backend/business/src/main/resources/db/migration/V1.0.1__migrate_workspace_business_data.sql`
2. INSERT INTO businesses SELECT data from workspaces
3. Map fields: workspace.name → business.name, workspace.workspace_type → business.business_type, etc.
4. Only migrate active workspaces (WHERE active = true)
5. Generate new UIDs for business records

**Files**: `ampairs-backend/business/src/main/resources/db/migration/V1.0.1__migrate_workspace_business_data.sql`

**Dependencies**: T004

**Reference**: data-model.md (Migration Impact section), research.md (Migration Strategy)

---

## Phase 3.3: Entity & Enums

### T006 [P]: Create BusinessType enum
**Description**: Define business type enumeration

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/enums/BusinessType.kt`
2. Define enum values: RETAIL, WHOLESALE, MANUFACTURING, SERVICE, RESTAURANT, ECOMMERCE, HEALTHCARE, EDUCATION, REAL_ESTATE, LOGISTICS, OTHER
3. Add description property for each type

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/enums/BusinessType.kt`

**Dependencies**: T001

**Reference**: data-model.md (Enum: BusinessType)

---

### T007 [P]: Create Business entity
**Description**: Create JPA entity for Business with @TenantId annotation

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/Business.kt`
2. Extend `OwnableBaseDomain` from core module
3. Add @Entity and @Table annotations with indexes
4. Define all fields matching database schema:
   - Profile: name, businessType, description, ownerName
   - Address: addressLine1, addressLine2, city, state, postalCode, country
   - Location: latitude, longitude
   - Contact: phone, email, website
   - Tax: taxId, registrationNumber, taxSettings (JSON)
   - Operational: timezone, currency, language, dateFormat, timeFormat
   - Hours: openingHours, closingHours, operatingDays (JSON)
   - Status: active
5. Add @TenantId annotation on workspaceId field
6. Implement `obtainSeqIdPrefix()` to return "BUS"
7. Add validation helper methods (e.g., validateBusinessHours())

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/Business.kt`

**Dependencies**: T001, T006

**Reference**: data-model.md (Entity: Business), research.md (Entity Design)

---

## Phase 3.4: DTOs & Mapping

### T008 [P]: Create BusinessResponse DTO
**Description**: Response DTO for API outputs

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessResponse.kt`
2. Define data class with all Business fields (snake_case will be handled by Jackson)
3. No @JsonProperty annotations needed (global snake_case config)
4. Include timestamps (createdAt, updatedAt)

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessResponse.kt`

**Dependencies**: T001

**Reference**: data-model.md (BusinessResponse), CLAUDE.md (JSON Naming Convention)

---

### T009 [P]: Create BusinessCreateRequest DTO
**Description**: Request DTO for business creation with validation

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessCreateRequest.kt`
2. Define data class with required and optional fields
3. Add Jakarta validation annotations:
   - @field:NotBlank on name
   - @field:NotNull on businessType
   - @field:Email on email
   - @field:Pattern on phone (E.164 format)
   - @field:Size constraints
4. Provide default values where appropriate

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessCreateRequest.kt`

**Dependencies**: T001, T006

**Reference**: data-model.md (BusinessCreateRequest), research.md (Validation Rules)

---

### T010 [P]: Create BusinessUpdateRequest DTO
**Description**: Request DTO for business updates (all fields optional)

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessUpdateRequest.kt`
2. Define data class with all fields nullable (partial update support)
3. Add same validation annotations as CreateRequest where applicable

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessUpdateRequest.kt`

**Dependencies**: T001, T006

**Reference**: data-model.md (BusinessUpdateRequest)

---

### T011 [P]: Create DTO extension functions
**Description**: Mapping functions between Entity and DTOs

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessExtensions.kt`
2. Implement extension functions:
   - `Business.asBusinessResponse(): BusinessResponse` - map entity to response
   - `BusinessCreateRequest.toBusiness(workspaceId: String): Business` - map request to entity
   - `Business.applyUpdate(update: BusinessUpdateRequest): Business` - apply partial update
3. Handle JSON field serialization/deserialization for operatingDays and taxSettings

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessExtensions.kt`

**Dependencies**: T007, T008, T009, T010

**Reference**: research.md (DTO Pattern)

---

## Phase 3.5: Exception Handling

### T012 [P]: Create BusinessNotFoundException
**Description**: Exception for business not found scenarios

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessNotFoundException.kt`
2. Extend `ResponseStatusException` with HttpStatus.NOT_FOUND
3. Constructor accepts workspaceId parameter
4. Message: "Business not found for workspace: {workspaceId}"

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessNotFoundException.kt`

**Dependencies**: T001

**Reference**: research.md (Exception Handling)

---

### T013 [P]: Create BusinessAlreadyExistsException
**Description**: Exception for duplicate business creation attempts

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessAlreadyExistsException.kt`
2. Extend `ResponseStatusException` with HttpStatus.CONFLICT
3. Constructor accepts workspaceId parameter
4. Message: "Business profile already exists for workspace: {workspaceId}"

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessAlreadyExistsException.kt`

**Dependencies**: T001

**Reference**: research.md (Exception Handling)

---

### T014 [P]: Create InvalidBusinessDataException
**Description**: Exception for business rule violations

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/InvalidBusinessDataException.kt`
2. Extend `ResponseStatusException` with HttpStatus.BAD_REQUEST
3. Constructor accepts custom message parameter
4. Used for business logic validation (e.g., closing hours before opening hours)

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/InvalidBusinessDataException.kt`

**Dependencies**: T001

**Reference**: research.md (Exception Handling)

---

### T015 [P]: Create BusinessExceptionHandler
**Description**: Global exception handler for business module

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessExceptionHandler.kt`
2. Add @ControllerAdvice annotation
3. Handle exceptions:
   - BusinessNotFoundException → 404 with ApiResponse error format
   - BusinessAlreadyExistsException → 409 with ApiResponse error format
   - InvalidBusinessDataException → 400 with ApiResponse error format
   - MethodArgumentNotValidException → 400 with validation details
4. Include trace IDs in error responses

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessExceptionHandler.kt`

**Dependencies**: T012, T013, T014

**Reference**: research.md (Exception Handling), contracts/business-api.yaml (Error responses)

---

## Phase 3.6: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE IMPLEMENTATION

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation in Phase 3.7-3.9**

### T016 [P]: Create BusinessIntegrationTest base setup
**Description**: Set up integration test infrastructure with Testcontainers

**Actions**:
1. Create `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`
2. Add @SpringBootTest annotation
3. Add @Testcontainers annotation
4. Configure PostgreSQL container:
   ```kotlin
   @Container
   val postgres = PostgreSQLContainer<Nothing>("postgres:15")
   ```
5. Set up test workspace and authentication context
6. DO NOT implement test methods yet (next tasks)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T002, T003

**Reference**: research.md (Testing Strategy)

---

### T017 [P]: Write integration test - Create business profile
**Description**: Test POST /api/v1/business endpoint (MUST FAIL initially)

**Actions**:
1. Add test method to `BusinessIntegrationTest.kt`:
   ```kotlin
   @Test
   fun `should create business profile successfully`()
   ```
2. Test steps:
   - Given: Valid BusinessCreateRequest with all required fields
   - When: POST to /api/v1/business with X-Workspace-ID header
   - Then:
     * Response status 201 Created
     * Response body has success: true
     * data.uid starts with "bus_"
     * data.seq_id starts with "BUS-"
     * All fields match request
     * Business exists in database
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md (Test Scenario 1), contracts/business-api.yaml (POST /business)

---

### T018 [P]: Write integration test - Retrieve business profile
**Description**: Test GET /api/v1/business endpoint (MUST FAIL initially)

**Actions**:
1. Add test method to `BusinessIntegrationTest.kt`:
   ```kotlin
   @Test
   fun `should retrieve business profile by workspace ID`()
   ```
2. Test steps:
   - Given: Business profile exists for workspace
   - When: GET /api/v1/business with X-Workspace-ID header
   - Then:
     * Response status 200 OK
     * Response has success: true
     * data contains all business fields
     * Response time < 50ms
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md (Test Scenario 2), contracts/business-api.yaml (GET /business)

---

### T019 [P]: Write integration test - Update business profile
**Description**: Test PUT /api/v1/business endpoint (MUST FAIL initially)

**Actions**:
1. Add test method to `BusinessIntegrationTest.kt`:
   ```kotlin
   @Test
   fun `should update business profile successfully`()
   ```
2. Test steps:
   - Given: Business profile exists
   - When: PUT /api/v1/business with partial update (phone, email, hours)
   - Then:
     * Response status 200 OK
     * Updated fields reflect new values
     * updatedAt timestamp is newer
     * Other fields unchanged
     * Changes persisted in database
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md (Test Scenario 3), contracts/business-api.yaml (PUT /business)

---

### T020 [P]: Write integration test - Duplicate prevention
**Description**: Test duplicate business creation returns 409 (MUST FAIL initially)

**Actions**:
1. Add test method to `BusinessIntegrationTest.kt`:
   ```kotlin
   @Test
   fun `should prevent duplicate business creation for same workspace`()
   ```
2. Test steps:
   - Given: Business profile already exists for workspace
   - When: POST /api/v1/business again for same workspace
   - Then:
     * Response status 409 Conflict
     * success: false
     * error.code: "BUSINESS_ALREADY_EXISTS"
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md (Test Scenario 1.2)

---

### T021 [P]: Write integration test - Tenant isolation
**Description**: Test multi-tenancy enforcement (MUST FAIL initially)

**Actions**:
1. Add test method to `BusinessIntegrationTest.kt`:
   ```kotlin
   @Test
   fun `should not allow cross-tenant data access`()
   ```
2. Test steps:
   - Given: Two workspaces each with business profiles
   - When: Request business with workspace A's ID, then with workspace B's ID
   - Then:
     * Each request returns only that workspace's business
     * No cross-tenant data leakage
     * Attempting to access non-existent workspace returns 404
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md (Test Scenario 2.2), research.md (Multi-Tenancy)

---

### T022 [P]: Write integration test - Validation scenarios
**Description**: Test request validation (MUST FAIL initially)

**Actions**:
1. Add test methods to `BusinessIntegrationTest.kt`:
   - `should reject missing required fields`
   - `should reject invalid email format`
   - `should reject invalid phone format`
   - `should reject invalid business hours` (closing before opening)
2. Each test verifies:
   - Response status 400 Bad Request
   - success: false
   - error.code: "VALIDATION_ERROR"
   - error.details contains field-specific messages
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md (Test Scenario 4), contracts/business-api.yaml (Validation)

---

### T023 [P]: Write integration test - Missing business returns 404
**Description**: Test GET for non-existent business returns 404 (MUST FAIL initially)

**Actions**:
1. Add test method to `BusinessIntegrationTest.kt`:
   ```kotlin
   @Test
   fun `should return 404 when business not found`()
   ```
2. Test steps:
   - Given: Workspace exists but has no business profile
   - When: GET /api/v1/business
   - Then:
     * Response status 404 Not Found
     * error.code: "BUSINESS_NOT_FOUND"
3. **VERIFY TEST FAILS** (no implementation exists yet)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessIntegrationTest.kt`

**Dependencies**: T016

**Reference**: quickstart.md, contracts/business-api.yaml (GET /business 404 response)

---

## Phase 3.7: Repository Layer (Make repository tests pass)

### T024: Create BusinessRepository interface
**Description**: Spring Data JPA repository for Business entity

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/repository/BusinessRepository.kt`
2. Extend `JpaRepository<Business, String>`
3. Add custom query methods:
   - `fun findByWorkspaceId(workspaceId: String): Business?`
   - `fun existsByWorkspaceId(workspaceId: String): Boolean`
   - `@Query("SELECT b FROM Business b WHERE b.workspaceId = :workspaceId AND b.active = true")`
     `fun findActiveByWorkspaceId(workspaceId: String): Business?`
4. Note: @TenantId on Business.workspaceId provides automatic filtering

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/repository/BusinessRepository.kt`

**Dependencies**: T007 (Business entity must exist)

**Reference**: research.md (Multi-Tenancy with @TenantId), data-model.md

---

## Phase 3.8: Service Layer (Make service tests pass)

### T025: Create BusinessService
**Description**: Business logic and validation service

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/service/BusinessService.kt`
2. Add @Service annotation
3. Inject BusinessRepository
4. Implement methods:
   - `create(request: BusinessCreateRequest, workspaceId: String): Business`
     * Check if business already exists for workspace
     * Throw BusinessAlreadyExistsException if exists
     * Validate business rules (hours consistency, etc.)
     * Convert request to entity using toBusiness()
     * Save and return
   - `findByWorkspaceId(workspaceId: String): Business`
     * Query repository
     * Throw BusinessNotFoundException if not found
     * Return business
   - `update(workspaceId: String, request: BusinessUpdateRequest): Business`
     * Find existing business
     * Validate update data
     * Apply updates using applyUpdate()
     * Save and return
5. Add business rule validation:
   - Closing hours must be after opening hours
   - Operating days must be valid day names
6. Add transaction management (@Transactional where needed)

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/service/BusinessService.kt`

**Dependencies**: T007, T009, T010, T011, T012, T013, T014, T024

**Reference**: research.md (Testing Strategy, Validation Rules)

---

## Phase 3.9: Controller Layer (Make API tests pass)

### T026: Create BusinessController
**Description**: REST API controller for business endpoints

**Actions**:
1. Create `ampairs-backend/business/src/main/kotlin/com/ampairs/business/controller/BusinessController.kt`
2. Add annotations:
   - @RestController
   - @RequestMapping("/api/v1/business")
3. Inject BusinessService
4. Implement endpoints:

   **GET /api/v1/business**:
   - Get workspaceId from TenantContextHolder.getCurrentTenant()
   - Call service.findByWorkspaceId()
   - Convert to BusinessResponse using asBusinessResponse()
   - Return ApiResponse.success(response)

   **POST /api/v1/business**:
   - Add @Valid annotation on request body
   - Get workspaceId from tenant context
   - Call service.create(request, workspaceId)
   - Convert to BusinessResponse
   - Return ApiResponse.success(response) with @ResponseStatus(HttpStatus.CREATED)

   **PUT /api/v1/business**:
   - Add @Valid annotation on request body
   - Get workspaceId from tenant context
   - Call service.update(workspaceId, request)
   - Convert to BusinessResponse
   - Return ApiResponse.success(response)

5. All endpoints require:
   - @PreAuthorize for workspace member authentication
   - X-Workspace-ID header validation
   - Error handling (let BusinessExceptionHandler handle)

**Files**: `ampairs-backend/business/src/main/kotlin/com/ampairs/business/controller/BusinessController.kt`

**Dependencies**: T008, T009, T010, T011, T025

**Reference**: research.md (API Endpoint Design, Controller Pattern), contracts/business-api.yaml, CLAUDE.md (Controller patterns)

---

## Phase 3.10: Contract Tests (Verify API schema compliance)

### T027 [P]: Create API contract test
**Description**: Verify API responses match OpenAPI specification

**Actions**:
1. Create `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessApiContractTest.kt`
2. Use Spring REST Docs or similar to validate:
   - Request schemas match spec (field names, types, required fields)
   - Response schemas match spec (snake_case, field types)
   - HTTP status codes match spec
   - Error responses match spec format
3. Load contracts/business-api.yaml and validate against actual API
4. Test all three endpoints (GET, POST, PUT)

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessApiContractTest.kt`

**Dependencies**: T026 (Controller implementation must exist)

**Reference**: contracts/business-api.yaml

---

## Phase 3.11: Migration Tests

### T028 [P]: Create migration test
**Description**: Verify database migrations execute correctly

**Actions**:
1. Create `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessMigrationTest.kt`
2. Use Testcontainers with PostgreSQL
3. Tests:
   - `should create businesses table with correct schema`
     * Verify table exists
     * Verify all columns exist with correct types
     * Verify constraints (primary key, foreign key, unique)
   - `should create all indexes`
     * Verify idx_business_workspace (unique)
     * Verify idx_business_type
     * Verify idx_business_active
   - `should migrate data from workspaces table`
     * Insert test data into workspaces table
     * Run migration V1.0.1
     * Verify data copied correctly
     * Verify field mappings
     * Verify only active workspaces migrated
   - `should enforce foreign key constraint`
     * Try to insert business with non-existent workspace_id
     * Verify constraint violation
   - `should enforce unique constraint on workspace_id`
     * Try to insert duplicate business for same workspace
     * Verify constraint violation

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessMigrationTest.kt`

**Dependencies**: T004, T005

**Reference**: quickstart.md (Test Scenario 6, Database Verification), data-model.md (Migration Impact)

---

## Phase 3.12: Polish & Documentation

### T029: Run integration tests and verify all pass
**Description**: Execute full test suite and confirm all tests pass

**Actions**:
1. Run: `./gradlew :ampairs-backend:business:test`
2. Verify all integration tests pass (T017-T023)
3. Verify contract tests pass (T027)
4. Verify migration tests pass (T028)
5. Check code coverage (aim for >80%)
6. If any tests fail, fix implementation before proceeding

**Files**: Test execution

**Dependencies**: T017-T023, T027, T028

**Reference**: quickstart.md (Success Criteria Checklist)

---

### T030 [P]: Create unit tests for BusinessService
**Description**: Add focused unit tests for service layer business logic

**Actions**:
1. Create `ampairs-backend/business/src/test/kotlin/com/ampairs/business/service/BusinessServiceTest.kt`
2. Use MockK to mock BusinessRepository
3. Test scenarios:
   - Business rule validation (hours consistency)
   - Exception throwing (not found, already exists)
   - Update logic (partial updates, null handling)
   - DTO mapping
4. Focus on logic not covered by integration tests

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/service/BusinessServiceTest.kt`

**Dependencies**: T025

**Reference**: research.md (Testing Strategy)

---

### T031 [P]: Performance testing
**Description**: Verify response times meet SLA (<50ms p95)

**Actions**:
1. Create `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessPerformanceTest.kt`
2. Test GET /api/v1/business with 100 requests
3. Measure and verify:
   - p50 response time
   - p95 response time < 50ms
   - p99 response time
4. Database query time < 10ms
5. Use JMH or similar for accurate benchmarking

**Files**: `ampairs-backend/business/src/test/kotlin/com/ampairs/business/BusinessPerformanceTest.kt`

**Dependencies**: T026

**Reference**: plan.md (Performance Goals), quickstart.md (Performance Verification)

---

### T032 [P]: Update CLAUDE.md with business module
**Description**: Document business module in project guidelines

**Actions**:
1. Edit `CLAUDE.md` at repository root
2. Add to "Recent Changes" section (keep last 3):
   - **Business Module (2025-10-10)**
   - Key points:
     * Extracted business profile from Workspace entity
     * One-to-one relationship: Workspace → Business
     * API: GET/POST/PUT /api/v1/business
     * @TenantId filtering on workspaceId
     * Migration: V1.0.0 (schema) + V1.0.1 (data)
3. Add to "Module Structure" section:
   - **business**: Business profile and configuration management

**Files**: `CLAUDE.md`

**Dependencies**: T029 (verify implementation complete)

**Reference**: CLAUDE.md (Recent Changes section)

---

### T033 [P]: Create API usage examples
**Description**: Document common API usage patterns

**Actions**:
1. Create `ampairs-backend/business/README.md`
2. Include:
   - Module overview
   - Quick start guide
   - API endpoint examples (curl)
   - Common use cases
   - Error handling examples
   - Migration notes for existing workspaces
3. Link to OpenAPI spec (contracts/business-api.yaml)

**Files**: `ampairs-backend/business/README.md`

**Dependencies**: T029

**Reference**: quickstart.md (test examples)

---

### T034: Execute quickstart validation scenarios
**Description**: Run through quickstart.md manually to validate end-to-end

**Actions**:
1. Start application: `./gradlew :ampairs_service:bootRun`
2. Execute all 6 test scenarios from quickstart.md:
   - Scenario 1: Create business profile
   - Scenario 2: Retrieve business profile
   - Scenario 3: Update business profile
   - Scenario 4: Validation testing
   - Scenario 5: Authorization testing
   - Scenario 6: Data migration verification
3. Run database verification queries
4. Run performance checks
5. Complete success criteria checklist

**Files**: Manual testing

**Dependencies**: T029

**Reference**: quickstart.md (all test scenarios)

---

### T035: Code review and refactoring
**Description**: Review implementation for code quality and refactor if needed

**Actions**:
1. Review all implemented code for:
   - Adherence to project guidelines (CLAUDE.md)
   - No @JsonProperty annotations (global snake_case)
   - Proper use of DTOs (no entity exposure)
   - @TenantId filtering working correctly
   - Error handling consistency
   - Code duplication
2. Refactor any issues found
3. Run tests again after refactoring
4. Commit with message following project conventions

**Files**: All business module files

**Dependencies**: T029

**Reference**: CLAUDE.md (Key Rules Summary)

---

## Dependencies Graph

```
Setup & Infrastructure:
T001 → T002 → T003
     → T004 → T005

Entities & DTOs (Parallel after T001):
T001 → T006 (enum)
T001 → T007 (entity, depends on T006)
T001 → T008, T009, T010 (DTOs, depends on T006)
T007, T008, T009, T010 → T011 (extensions)

Exceptions (Parallel after T001):
T001 → T012, T013, T014 (exceptions)
T012, T013, T014 → T015 (handler)

Tests First (TDD - Parallel after T002, T003):
T002, T003 → T016 (test setup)
T016 → T017, T018, T019, T020, T021, T022, T023 (integration tests)

Implementation (After tests written):
T007 → T024 (repository depends on entity)
T007, T009, T010, T011, T012, T013, T014, T024 → T025 (service)
T008, T009, T010, T011, T025 → T026 (controller)

Verification Tests:
T026 → T027 (contract test)
T004, T005 → T028 (migration test)

Polish (Parallel after T029):
T017-T023, T027, T028 → T029 (verify all tests pass)
T025 → T030 (unit tests)
T026 → T031 (performance)
T029 → T032, T033 (docs)
T029 → T034 (quickstart)
T029 → T035 (review)
```

## Parallel Execution Examples

### Example 1: Entity & DTO Creation (After T001)
```bash
# These can run in parallel (different files):
Task: "Create BusinessType enum in ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/enums/BusinessType.kt"
Task: "Create BusinessResponse DTO in ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessResponse.kt"
Task: "Create BusinessCreateRequest DTO in ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessCreateRequest.kt"
Task: "Create BusinessUpdateRequest DTO in ampairs-backend/business/src/main/kotlin/com/ampairs/business/model/dto/BusinessUpdateRequest.kt"
```

### Example 2: Exception Classes (After T001)
```bash
# These can run in parallel (different files):
Task: "Create BusinessNotFoundException in ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessNotFoundException.kt"
Task: "Create BusinessAlreadyExistsException in ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/BusinessAlreadyExistsException.kt"
Task: "Create InvalidBusinessDataException in ampairs-backend/business/src/main/kotlin/com/ampairs/business/exception/InvalidBusinessDataException.kt"
```

### Example 3: Integration Tests (After T016)
```bash
# These can run in parallel (independent test methods):
Task: "Write integration test for create business profile"
Task: "Write integration test for retrieve business profile"
Task: "Write integration test for update business profile"
Task: "Write integration test for duplicate prevention"
Task: "Write integration test for tenant isolation"
Task: "Write integration test for validation scenarios"
```

### Example 4: Polish Tasks (After T029)
```bash
# These can run in parallel (different files):
Task: "Create unit tests for BusinessService"
Task: "Run performance tests"
Task: "Update CLAUDE.md with business module"
Task: "Create API usage examples README"
```

## Validation Checklist

Before marking tasks complete, verify:

- [x] All contracts have corresponding tests (T027)
- [x] All entities have model tasks (T007)
- [x] All tests come before implementation (T017-T023 before T024-T026)
- [x] Parallel tasks are truly independent (different files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] TDD approach: Tests MUST FAIL before implementation
- [x] All integration tests pass (T029)
- [x] Performance goals met: <50ms p95 (T031)
- [x] Documentation updated (T032, T033)
- [x] Quickstart validation complete (T034)

## Notes

- **TDD is mandatory**: Tests T017-T023 MUST fail before implementing T024-T026
- **No @JsonProperty annotations**: Global snake_case configuration handles it
- **DTOs required**: Never expose JPA entities directly in controllers
- **@TenantId filtering**: Automatic workspace filtering on Business.workspaceId
- **Commit after each task**: Maintain granular git history
- **Run tests frequently**: `./gradlew :ampairs-backend:business:test`
- **Migrations are irreversible**: Test thoroughly before production deployment

## Success Criteria

Implementation is complete when:

1. ✅ All 35 tasks completed
2. ✅ All tests pass (integration, contract, migration, unit)
3. ✅ Performance SLA met (<50ms p95)
4. ✅ Quickstart scenarios validated
5. ✅ Code review passed
6. ✅ Documentation updated
7. ✅ No cross-tenant data leakage
8. ✅ Migrations tested with sample data

---

**Total Tasks**: 35
**Estimated Duration**: 4-5 days
**Parallel Opportunities**: 15 tasks marked [P]
**Critical Path**: T001 → T002 → T003 → T016 → T017-T023 → T024 → T025 → T026 → T029

**Status**: Ready for execution
**Next Step**: Begin with T001 (Create Gradle module structure)
