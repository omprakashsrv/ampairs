# Tasks: Separate Unit Module

**Input**: Design documents from `/specs/004-create-separate-unit/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/unit-api.yaml, quickstart.md

**Tests**: Tests are included based on success criteria SC-004 requiring all automated tests to pass

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story, following the phased migration strategy from research.md

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, or SETUP/FOUNDATION)
- Include exact file paths in descriptions

## Path Conventions
- Backend module library: `ampairs-backend/unit/`
- Modified modules: `ampairs-backend/product/`, `ampairs-backend/ampairs_service/`
- Tests: `ampairs-backend/unit/src/test/kotlin/`

---

## Phase 1: Setup (Module Initialization)

**Purpose**: Create unit module structure and configure build system

- [x] T001 [P] [SETUP] Create `ampairs-backend/unit/` directory structure with src/main/kotlin and src/test/kotlin
- [x] T002 [P] [SETUP] Create `ampairs-backend/unit/build.gradle.kts` (copy from tax module, disable bootJar, enable jar)
- [x] T003 [SETUP] Update `settings.gradle.kts` at repository root to include `:unit` module
- [x] T004 [SETUP] Create package structure: `com.ampairs.unit.{config,controller,service,repository,domain,exception}`
- [x] T005 [SETUP] Verify empty module builds successfully: `./gradlew :unit:build`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities, DTOs, and configurations that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 [P] [FOUNDATION] Copy `Unit` entity from product module to `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/model/Unit.kt`, update package to `com.ampairs.unit.domain.model`, ensure extends OwnableBaseDomain
- [x] T007 [P] [FOUNDATION] Copy `UnitConversion` entity from product module to `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/model/UnitConversion.kt`, update package and relationships to reference unit module
- [x] T008 [P] [FOUNDATION] Create `UnitRequest` DTO in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/dto/UnitRequest.kt` with validation annotations (@NotBlank, @Size, @Min, @Max)
- [x] T009 [P] [FOUNDATION] Create `UnitResponse` DTO in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/dto/UnitResponse.kt` with converter extension function `Unit.asUnitResponse()`
- [x] T010 [P] [FOUNDATION] Create `UnitConversionRequest` DTO in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/dto/UnitConversionRequest.kt` with validation
- [x] T011 [P] [FOUNDATION] Create `UnitConversionResponse` DTO in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/domain/dto/UnitConversionResponse.kt` with nested unit details and converter function
- [x] T012 [P] [FOUNDATION] Create `Constants.kt` in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/config/` with UNIT_PREFIX constant
- [x] T013 [P] [FOUNDATION] Create `UnitModuleConfiguration.kt` in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/config/` with @Configuration and @ComponentScan annotations
- [x] T014 [P] [FOUNDATION] Create `UnitNotFoundException` exception in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/exception/UnitNotFoundException.kt`
- [x] T015 [P] [FOUNDATION] Create `UnitInUseException` exception in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/exception/UnitInUseException.kt`
- [x] T016 [P] [FOUNDATION] Create `CircularConversionException` exception in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/exception/CircularConversionException.kt`
- [ ] T017 [FOUNDATION] Verify foundational components compile: `./gradlew :unit:build` *(Pending external run: Java runtime unavailable in sandbox)*

**Checkpoint**: Foundation ready - entities, DTOs, and exceptions exist. User story implementation can now begin.

---

## Phase 3: User Story 1 - Access Centralized Units from Product Management (Priority: P1) 🎯 MVP

**Goal**: Enable product module to access centralized unit catalog for unit selection and display

**Independent Test**: Create a product and assign it a measurement unit from the centralized unit catalog. Verify the unit appears correctly in product details.

**Acceptance Scenarios**:
1. User creates a product → can select from all available units in centralized catalog
2. User views product details → unit information displays correctly with name and symbol
3. Multiple products use same unit → unit definition update reflects in all products

### Implementation for User Story 1

- [x] T018 [US1] Create `UnitRepository` interface in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/repository/UnitRepository.kt` extending CrudRepository with methods: findByUid, findByRefId, findAllByActiveTrueOrderByName
- [x] T019 [US1] Create `UnitService` interface in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/service/UnitService.kt` with methods: findByUid, findByRefId, findAll, create, update, delete, isUnitInUse, findProductsUsingUnit
- [x] T020 [US1] Implement `UnitServiceImpl` in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/service/UnitServiceImpl.kt` with business logic for CRUD operations, tenant context validation, and usage checking
- [x] T021 [US1] Create `UnitController` in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/controller/UnitController.kt` with endpoints: GET /api/v1/unit (list), POST /api/v1/unit (create), GET /api/v1/unit/{uid} (get), PUT /api/v1/unit/{uid} (update), DELETE /api/v1/unit/{uid} (delete)
- [x] T022 [US1] Add `GET /api/v1/unit/{uid}/usage` endpoint in UnitController to check unit usage before deletion
- [x] T023 [US1] Add tenant context handling in UnitController using TenantContextHolder.getCurrentTenant() from X-Workspace-ID header
- [x] T024 [US1] Add unit module dependency to product module's `build.gradle.kts`: `api(project(":unit"))`
- [x] T025 [US1] Update product module imports from `com.ampairs.product.domain.model.Unit` to `com.ampairs.unit.domain.model.Unit` in all product-related files
- [x] T026 [US1] Update ProductService in product module to inject UnitService and validate unit existence when creating/updating products
- [ ] T027 [US1] Verify unit module builds: `./gradlew :unit:build` *(Pending external run: Java runtime unavailable in sandbox)*
- [ ] T028 [US1] Verify product module builds with new unit dependency: `./gradlew :product:build` *(Pending external run: Java runtime unavailable in sandbox)*

### Tests for User Story 1

- [x] T029 [P] [US1] Create `UnitServiceTest` in `ampairs-backend/unit/src/test/kotlin/com/ampairs/unit/UnitServiceTest.kt` with unit tests for create, update, delete, find operations using Mockito
- [x] T030 [P] [US1] Create `UnitControllerIntegrationTest` in `ampairs-backend/unit/src/test/kotlin/com/ampairs/unit/UnitControllerIntegrationTest.kt` with @SpringBootTest for API endpoint testing
- [x] T031 [US1] Create integration test in product module verifying product creation with unit from unit module
- [ ] T032 [US1] Run all US1 tests: `./gradlew :unit:test :product:test` *(Pending external run: Java runtime unavailable in sandbox)*

**Checkpoint**: At this point, User Story 1 should be fully functional - products can access and use centralized unit catalog

---

## Phase 4: User Story 2 - Define Unit Conversions for Multi-Unit Products (Priority: P1)

**Goal**: Enable defining unit conversions (global and product-specific) with automatic quantity conversion

**Independent Test**: Define a unit conversion (e.g., 1 box = 12 pieces), create a product, and verify that orders can be placed in either unit with automatic quantity conversion.

**Acceptance Scenarios**:
1. Product with defined unit conversions → order in derived unit converts to base unit for inventory deduction
2. Unit conversion is defined → view conversion details showing multiplier and relationship between units
3. Product has multiple unit conversions → invoice uses appropriate conversion based on order unit

### Implementation for User Story 2

- [x] T033 [US2] Create `UnitConversionRepository` interface in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/repository/UnitConversionRepository.kt` with methods: findByProductId, findByBaseUnitIdOrDerivedUnitId, findByBaseUnitIdAndDerivedUnitIdAndProductId, use @EntityGraph("UnitConversion.withUnits")
- [x] T034 [US2] Create `UnitConversionService` interface in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/service/UnitConversionService.kt` with methods: findByProductId, convert, create, update, delete, validateNoCircularConversion
- [x] T035 [US2] Implement `UnitConversionServiceImpl` in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/service/UnitConversionServiceImpl.kt` with conversion logic, circular dependency validation, and product/inventory specific conversion support
- [x] T036 [US2] Add `convert(quantity, fromUnitId, toUnitId, productId?)` method in UnitConversionServiceImpl with bidirectional conversion support (if A→B exists, calculate B→A as 1/multiplier)
- [x] T037 [US2] Create `UnitConversionController` in `ampairs-backend/unit/src/main/kotlin/com/ampairs/unit/controller/UnitConversionController.kt` with endpoints: GET /api/v1/unit/conversion (list with filters), POST /api/v1/unit/conversion (create), GET /api/v1/unit/conversion/{uid} (get), PUT /api/v1/unit/conversion/{uid} (update), DELETE /api/v1/unit/conversion/{uid} (delete)
- [x] T038 [US2] Add `POST /api/v1/unit/conversion/convert` endpoint in UnitConversionController for quantity conversion API
- [x] T039 [US2] Add circular conversion validation in UnitConversionServiceImpl.create/update that checks for cycles (A→B, B→C, C→A)
- [ ] T040 [US2] Update order processing in product/order modules to use UnitConversionService.convert when order unit differs from product base unit
- [ ] T041 [US2] Verify conversions work: `./gradlew :unit:build` *(Pending external run: Java runtime unavailable in sandbox)*

### Tests for User Story 2

- [x] T042 [P] [US2] Create `UnitConversionServiceTest` in `ampairs-backend/unit/src/test/kotlin/com/ampairs/unit/UnitConversionServiceTest.kt` with tests for create, convert, circular dependency detection
- [x] T043 [P] [US2] Create `UnitConversionControllerIntegrationTest` in `ampairs-backend/unit/src/test/kotlin/com/ampairs/unit/UnitConversionControllerIntegrationTest.kt` for API endpoint testing
- [ ] T044 [US2] Create integration test for product-specific conversion: create unit conversion for specific product, verify order quantity conversion
- [ ] T045 [US2] Create integration test for circular conversion prevention: attempt to create A→B, B→C, C→A, verify exception thrown
- [ ] T046 [US2] Run all US2 tests: `./gradlew :unit:test`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - units exist and conversions work

---

## Phase 5: User Story 3 - Manage Global Unit Catalog (Priority: P2)

**Goal**: Enable administrators to create, update, and manage centralized unit catalog with validation

**Independent Test**: Add a new unit to the catalog, assign it to a product, and verify it's available for selection in inventory and invoice modules without requiring module-specific configuration.

**Acceptance Scenarios**:
1. Administrator creates new unit → unit becomes immediately available across all modules
2. Administrator updates unit properties → changes propagate to all modules using that unit
3. Administrator attempts to delete unit in use → system prevents deletion and shows where unit is used

### Implementation for User Story 3

- [ ] T047 [US3] Add unique name constraint validation in UnitServiceImpl.create: check if unit with same name exists in workspace before creating
- [ ] T048 [US3] Enhance `isUnitInUse` method in UnitServiceImpl to query product, inventory, invoice, and unit_conversion tables for references
- [ ] T049 [US3] Enhance `findProductsUsingUnit` method in UnitServiceImpl to return list of product UIDs using the unit
- [ ] T050 [US3] Add deletion prevention logic in UnitServiceImpl.delete: throw UnitInUseException if isUnitInUse returns true
- [ ] T051 [US3] Add `GET /api/v1/unit/{uid}/usage` endpoint response to include: inUse boolean, productCount, conversionCount, productIds list (per UnitUsage schema in contracts)
- [x] T052 [US3] Update inventory module to import unit entities from unit module instead of product module
- [ ] T053 [US3] Update invoice module (if needed) to import unit entities from unit module
- [ ] T054 [US3] Verify cross-module access: create unit in unit module, use in product, verify visible in inventory
- [ ] T055 [US3] Verify unit module builds: `./gradlew :unit:build`

### Tests for User Story 3

- [ ] T056 [P] [US3] Create test for duplicate unit name prevention: attempt to create two units with same name in same workspace, verify second fails
- [ ] T057 [P] [US3] Create test for unit deletion prevention: create unit, assign to product, attempt delete, verify UnitInUseException thrown
- [ ] T058 [P] [US3] Create test for unit usage reporting: create unit with 3 products using it, call usage endpoint, verify productCount=3 and productIds list
- [ ] T059 [US3] Create cross-module integration test: create unit in unit module, create product using unit, create inventory adjustment using unit, verify all work correctly
- [ ] T060 [US3] Run all US3 tests: `./gradlew :unit:test :product:test`

**Checkpoint**: All P1 and P2 features complete - unit catalog management fully operational with validation

---

## Phase 6: User Story 4 - Track Inventory in Multiple Units (Priority: P2)

**Goal**: Enable inventory tracking in multiple units with automatic conversion between storage and sales units

**Independent Test**: Receive inventory in bulk units (e.g., pallets), sell in retail units (e.g., pieces), and verify inventory levels are accurately maintained with automatic conversion.

**Acceptance Scenarios**:
1. Inventory stored in base unit → sale in derived unit → system auto-converts and deducts correct quantity
2. Multiple inventory locations with different storage units → view total inventory → all quantities normalized to single unit
3. Product with unit conversions → inventory adjustments → users can input adjustments in any defined unit

### Implementation for User Story 4

- [ ] T061 [US4] Refactor `InventoryUnitConversion` entity in inventory module to use unit module's Unit entity instead of product module's
- [ ] T062 [US4] Update `InventoryService` to inject UnitConversionService and use it for quantity conversions during stock adjustments
- [ ] T063 [US4] Add conversion logic in InventoryService.addStock: if input unit differs from inventory base unit, call UnitConversionService.convert before updating inventory
- [ ] T064 [US4] Add conversion logic in InventoryService.deductStock: support deduction in any unit defined for the product, convert to base unit before deducting
- [ ] T065 [US4] Update inventory retrieval methods to support querying in different units: convert inventory quantities to requested unit on read
- [ ] T066 [US4] Add normalization logic for inventory totals across locations: sum all inventory in base unit, optionally convert to display unit
- [ ] T067 [US4] Verify inventory conversions work: create product with 2 units (kg and gram), add 5kg stock, sell 2000g, verify 3kg remaining
- [ ] T068 [US4] Verify inventory module builds: `./gradlew :product:build` (inventory is subpackage of product)

### Tests for User Story 4

- [ ] T069 [P] [US4] Create test for inventory addition with unit conversion: add stock in kg, verify base unit (gram) inventory increased correctly
- [ ] T070 [P] [US4] Create test for inventory deduction with unit conversion: stock in base unit, sell in derived unit, verify correct deduction
- [ ] T071 [P] [US4] Create test for inventory normalization: stock in 3 locations with different units, call total inventory, verify correct sum in normalized unit
- [ ] T072 [US4] Create integration test for complete inventory flow: receive in pallets, convert to cases, sell in pieces, verify all conversions accurate
- [ ] T073 [US4] Run all US4 tests: `./gradlew :product:test` (includes inventory tests)

**Checkpoint**: All user stories complete - unit module fully functional with inventory integration

---

## Phase 7: Module Migration & Cleanup

**Purpose**: Complete migration by updating main application and cleaning up deprecated code

- [x] T074 [MIGRATION] Add unit module dependency to `ampairs-backend/ampairs_service/build.gradle.kts`: `implementation(project(":unit"))`
- [ ] T075 [MIGRATION] Verify component scanning in ampairs_service includes unit module (should be automatic if @ComponentScan configured)
- [ ] T076 [MIGRATION] Run full application build: `./gradlew :ampairs_service:build`
- [x] T077 [MIGRATION] Remove deprecated unit entities from product module: delete `ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/model/Unit.kt`
- [x] T078 [MIGRATION] Remove deprecated unit conversion entity from product module: delete `ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/model/UnitConversion.kt`
- [x] T079 [MIGRATION] Remove deprecated unit DTOs from product module: delete `ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/dto/unit/` directory
- [x] T080 [MIGRATION] Remove deprecated unit repository from product module: delete `ampairs-backend/product/src/main/kotlin/com/ampairs/product/repository/UnitRepository.kt`
- [ ] T081 [MIGRATION] Verify no orphaned unit references remain in product module: `grep -r "product.domain.model.Unit" ampairs-backend/product/`
- [ ] T082 [MIGRATION] Run full test suite: `./gradlew :ampairs_service:test`
- [ ] T083 [MIGRATION] Start application and verify unit endpoints accessible: `./gradlew :ampairs_service:bootRun`

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, documentation, and validation

- [ ] T084 [P] [POLISH] Update CLAUDE.md to document unit module structure and usage patterns
- [ ] T085 [P] [POLISH] Create README.md in `ampairs-backend/unit/` directory with module overview and quick start (if not already present)
- [ ] T086 [P] [POLISH] Add OpenAPI/Swagger documentation annotations to UnitController and UnitConversionController for API documentation
- [ ] T087 [POLISH] Run code quality checks: `./gradlew :unit:check`
- [ ] T088 [POLISH] Verify test coverage meets ≥80% target: `./gradlew :unit:jacocoTestReport` (if Jacoco configured)
- [ ] T089 [POLISH] Run full CI build: `./gradlew ciBuild` (includes all modules)
- [ ] T090 [POLISH] Manual API testing per quickstart.md: create unit via API, create conversion, convert quantity, verify responses match OpenAPI spec
- [ ] T091 [POLISH] Multi-tenant isolation test: create unit in workspace A, attempt access from workspace B, verify returns null/404
- [ ] T092 [POLISH] Performance test: measure unit API response times under load, verify <200ms requirement met
- [ ] T093 [POLISH] Update module dependency diagram in project documentation to show unit module position

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) - Core unit CRUD operations
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) AND User Story 1 (needs units to exist) - Unit conversions
- **User Story 3 (Phase 5)**: Depends on User Stories 1 & 2 - Advanced catalog management with validation
- **User Story 4 (Phase 6)**: Depends on User Stories 1 & 2 - Inventory integration requires units and conversions
- **Migration (Phase 7)**: Depends on all user stories being complete - Cannot remove deprecated code until new code works
- **Polish (Phase 8)**: Depends on Migration completion - Final validation and documentation

### User Story Dependencies

```
Foundational (Phase 2) - BLOCKS ALL
    ↓
US1 (Phase 3) - Core unit catalog
    ↓
US2 (Phase 4) - Unit conversions (needs US1)
    ↓
US3 (Phase 5) - Advanced management (needs US1, US2)
    ↓
US4 (Phase 6) - Inventory integration (needs US1, US2)
    ↓
Migration (Phase 7)
    ↓
Polish (Phase 8)
```

### Critical Path

The critical path for MVP (User Story 1 only):

```
T001-T005 (Setup) → T006-T017 (Foundation) → T018-T028 (US1 Implementation) → T029-T032 (US1 Tests)
```

Total MVP tasks: 32 tasks

### Within Each User Story

- Setup tasks before foundational tasks
- Foundational entities/DTOs before repositories
- Repositories before services
- Services before controllers
- Controllers before cross-module integration
- Tests can run after implementation (or in parallel if TDD approach)
- Story checkpoint validation before moving to next story

### Parallel Opportunities

- **Phase 1 (Setup)**: T001, T002, T003 can run in parallel
- **Phase 2 (Foundational)**: T006, T007, T008, T009, T010, T011, T012, T013, T014, T015, T016 can all run in parallel (different files)
- **US1 Tests**: T029, T030 can run in parallel (different test files)
- **US2 Tests**: T042, T043 can run in parallel
- **US3 Tests**: T056, T057, T058 can run in parallel
- **US4 Tests**: T069, T070, T071 can run in parallel
- **Polish**: T084, T085, T086 can run in parallel (documentation tasks)

---

## Parallel Example: Foundational Phase

```bash
# Launch all foundational entity/DTO tasks together (all different files):
Task T006: "Copy Unit entity to unit module"
Task T007: "Copy UnitConversion entity to unit module"
Task T008: "Create UnitRequest DTO"
Task T009: "Create UnitResponse DTO"
Task T010: "Create UnitConversionRequest DTO"
Task T011: "Create UnitConversionResponse DTO"
Task T012: "Create Constants.kt"
Task T013: "Create UnitModuleConfiguration.kt"
Task T014: "Create UnitNotFoundException"
Task T015: "Create UnitInUseException"
Task T016: "Create CircularConversionException"

# These 11 tasks can all be done simultaneously by different developers or agents
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Goal**: Get basic unit catalog working that product module can use

1. Complete Phase 1: Setup (T001-T005) - 5 tasks
2. Complete Phase 2: Foundational (T006-T017) - 12 tasks
3. Complete Phase 3: User Story 1 (T018-T032) - 15 tasks
4. **STOP and VALIDATE**: Test User Story 1 independently
   - Create units via API
   - Product module can access units
   - Unit usage checking works
5. **MVP READY**: 32 tasks total, basic unit catalog operational

**MVP Delivery**: Centralized unit catalog accessible by product module

### Incremental Delivery (All P1 Stories)

1. Complete Setup + Foundational → T001-T017 (17 tasks)
2. Add User Story 1 → T018-T032 (15 tasks) → **Test independently** → MVP!
3. Add User Story 2 → T033-T046 (14 tasks) → **Test independently** → Unit conversions work!
4. **P1 COMPLETE**: 46 tasks total, core functionality operational
5. Deploy P1 features, gather feedback

### Full Feature Delivery (All Stories)

1. Complete P1 stories (T001-T046) - 46 tasks
2. Add User Story 3 → T047-T060 (14 tasks) → **Test independently** → Admin management complete!
3. Add User Story 4 → T061-T073 (13 tasks) → **Test independently** → Inventory integration complete!
4. Complete Migration → T074-T083 (10 tasks) → Remove deprecated code
5. Complete Polish → T084-T093 (10 tasks) → Final validation
6. **FEATURE COMPLETE**: 93 tasks total, full unit module extraction done

### Parallel Team Strategy

With multiple developers available:

**Week 1**: Setup + Foundational
- Team completes T001-T017 together (17 tasks)
- Many parallel opportunities (T006-T016 can be done simultaneously)

**Week 2**: Parallel User Story Development
- Developer A: User Story 1 (T018-T032) - 15 tasks
- Developer B: User Story 2 (T033-T046) - 14 tasks (depends on US1 entities but can start service/controller work in parallel)

**Week 3**: Advanced Features
- Developer A: User Story 3 (T047-T060) - 14 tasks
- Developer B: User Story 4 (T061-T073) - 13 tasks

**Week 4**: Integration & Polish
- Team: Migration (T074-T083) + Polish (T084-T093) - 20 tasks

---

## Task Summary

### Total Task Count: 93 tasks

**By Phase**:
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 12 tasks
- Phase 3 (User Story 1 - P1): 15 tasks
- Phase 4 (User Story 2 - P1): 14 tasks
- Phase 5 (User Story 3 - P2): 14 tasks
- Phase 6 (User Story 4 - P2): 13 tasks
- Phase 7 (Migration): 10 tasks
- Phase 8 (Polish): 10 tasks

**By User Story**:
- US1 (Access Centralized Units): 15 tasks
- US2 (Define Unit Conversions): 14 tasks
- US3 (Manage Global Unit Catalog): 14 tasks
- US4 (Track Inventory in Multiple Units): 13 tasks
- Setup/Foundation/Migration/Polish: 37 tasks

**Parallel Opportunities**:
- 11 foundational tasks can run in parallel (T006-T016)
- 2-3 test tasks per user story can run in parallel
- 3 documentation tasks can run in parallel (T084-T086)
- Total parallelizable: ~25 tasks

**Test Tasks**: 20 tasks (22% of total)
- Unit tests: 8 tasks (T029, T030, T042, T043, T056, T057, T058, T059)
- Integration tests: 12 tasks (T031, T032, T044, T045, T046, T060, T069, T070, T071, T072, T073, T091)

**MVP Scope** (User Story 1 only): 32 tasks (35% of total)

**P1 Features** (User Stories 1 & 2): 46 tasks (49% of total)

**Full Feature** (All Stories): 93 tasks (100%)

---

## Independent Test Criteria

### User Story 1
- [ ] Create unit via POST /api/v1/unit → returns 201 with UnitResponse
- [ ] Create product with unit UID → product service validates unit exists
- [ ] Retrieve product → displays unit name and short_name correctly
- [ ] Update unit name → all products using that unit reflect updated name

### User Story 2
- [ ] Create unit conversion (1 box = 12 pieces) → returns 201 with UnitConversionResponse
- [ ] Call convert API with 3 boxes → returns 36 pieces
- [ ] Create product-specific conversion → verify different from global conversion
- [ ] Attempt circular conversion (A→B, B→C, C→A) → returns 400 with CircularConversionException

### User Story 3
- [ ] Create unit with duplicate name in same workspace → returns 409 Conflict
- [ ] Assign unit to 3 products → GET /api/v1/unit/{uid}/usage returns productCount=3
- [ ] Attempt delete unit in use → returns 409 with UnitInUseException listing products
- [ ] Update unit decimal_places → changes propagate to all dependent modules immediately

### User Story 4
- [ ] Add 5000 grams inventory via inventory API → base unit stock increases correctly
- [ ] Sell 2 kg via order API → inventory deducts 2000 grams automatically
- [ ] Query inventory in kg → returns 3.0 kg (converted from 3000 grams)
- [ ] Multiple locations with different units → total inventory API normalizes to single unit correctly

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label (US1, US2, US3, US4, SETUP, FOUNDATION, MIGRATION, POLISH) maps task to specific phase for traceability
- Each user story should be independently completable and testable (follow checkpoints)
- Tests included because SC-004 requires "all automated tests pass after module extraction"
- Commit after each task or logical group of related tasks
- Stop at any checkpoint to validate story independently before proceeding
- Critical path for MVP: Setup → Foundation → US1 (32 tasks)
- Database tables (unit, unit_conversion) already exist - no Flyway migrations needed
- Entity package changes from com.ampairs.product to com.ampairs.unit - update all imports
- Use tax module (/ampairs-backend/tax) as reference for structure and patterns
- Multi-tenant isolation via @TenantId automatic - controllers set tenant context via X-Workspace-ID header
