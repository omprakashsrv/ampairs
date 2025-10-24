# Research: Unit Module Extraction

**Feature**: Separate Unit Module
**Date**: 2025-10-12
**Phase**: 0 - Research & Analysis

## Overview

This document captures research findings for extracting unit management functionality from the product module into a dedicated unit module. The extraction follows the established pattern from the tax module extraction and adheres to all Ampairs constitution principles.

## Research Areas

### 1. Tax Module Extraction Pattern (Reference Architecture)

**Decision**: Follow the tax module structure exactly

**Analysis of Tax Module**:
- **Location**: `/ampairs-backend/tax/`
- **Package Structure**: `com.ampairs.tax.{layer}`
- **Layers**:
  - `config/` - Module configuration (TaxModuleConfiguration, Constants)
  - `controller/` - REST endpoints (TaxRateController, HsnCodeController, TaxCalculationController, TaxConfigurationController)
  - `service/` - Business logic (TaxRateService, HsnCodeService, GstTaxCalculationService, TaxConfigurationService)
  - `repository/` - Data access (TaxRateRepository, HsnCodeRepository, TaxConfigurationRepository, BusinessTypeRepository)
  - `domain/model/` - JPA entities
  - `domain/dto/` - Request/Response DTOs
  - `domain/enums/` - Enumeration types (TaxComponentType, BusinessType)
  - `exception/` - Custom exceptions

**Build Configuration** (from tax/build.gradle.kts):
```kotlin
plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.2.20"
}

// Disable bootJar since this module doesn't have a main class
tasks.named("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}

dependencies {
    api(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // ... etc
}
```

**Rationale**: The tax module provides a proven, working template that already passes all constitution checks. Replicating this structure minimizes risk and ensures consistency across the codebase.

**Alternatives Considered**:
- Create a microservice: Rejected because unit module doesn't need independent deployment; adds unnecessary complexity
- Keep in product module: Rejected because violates single responsibility principle and creates unnecessary coupling

---

### 2. Existing Unit Code Analysis

**Location of Existing Code**:

**Entities** (in product module):
- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/model/Unit.kt`
  - Fields: name, shortName, decimalPlaces
  - Extends: OwnableBaseDomain
  - Table: `unit`
  - Indexes: name, uid (unique)

- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/model/UnitConversion.kt`
  - Fields: baseUnitId, derivedUnitId, productId, multiplier
  - Relationships: @OneToOne to baseUnit, derivedUnit, product
  - Extends: OwnableBaseDomain
  - Table: `unit_conversion`

**DTOs** (in product module):
- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/dto/unit/UnitRequest.kt`
- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/dto/unit/UnitResponse.kt`
- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/dto/unit/UnitConversionRequest.kt`
- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/domain/dto/unit/UnitConversionResponse.kt`

**Repository** (in product module):
- `/ampairs-backend/product/src/main/kotlin/com/ampairs/product/repository/UnitRepository.kt`
  - Methods: findByUid, findByRefId
  - Extends: CrudRepository<Unit, Long>

**Dependencies**:
- Product module: References Unit via unitId foreign keys
- Inventory module: InventoryUnitConversion entity depends on Unit entity
- Invoice module: Minimal dependency (only in test data)

**Decision**: Move all unit-related code to new unit module while preserving exact functionality

**Rationale**: Clean separation allows independent evolution of unit management without affecting product catalog logic.

---

### 3. Module Dependency Strategy

**Decision**: Reverse dependency direction

**Current State**:
```
core ← product (contains unit code)
           ↓
      inventory, invoice (depend on product for units)
```

**Target State**:
```
core ← unit (new module)
         ↓
      product, inventory, invoice (depend on unit module)
```

**Gradle Configuration Changes Required**:

1. **Create** `ampairs-backend/unit/build.gradle.kts`:
```kotlin
dependencies {
    api(project(":core"))
    // Standard Spring Boot dependencies (same as tax module)
}
```

2. **Modify** `ampairs-backend/product/build.gradle.kts`:
```kotlin
dependencies {
    api(project(":core"))
    api(project(":unit"))  // ADD THIS
    // ... rest unchanged
}
```

3. **Modify** `ampairs-backend/ampairs_service/build.gradle.kts`:
```kotlin
dependencies {
    // ... existing dependencies
    implementation(project(":unit"))  // ADD THIS
}
```

4. **Update** `settings.gradle.kts` (root):
```kotlin
include(
    ":core",
    ":auth",
    ":workspace",
    ":tax",
    ":unit",  // ADD THIS
    ":product",
    // ... rest
)
```

**Rationale**: This ensures unit module has no product-specific dependencies while product can depend on unit, following proper layering.

**Alternatives Considered**:
- Circular dependency: Rejected (violates layered architecture)
- Shared library: Rejected (adds unnecessary indirection)

---

### 4. Database Schema Considerations

**Decision**: No database migration needed; tables remain unchanged

**Analysis**:
- Database tables (`unit`, `unit_conversion`) already exist
- Foreign key relationships from product/inventory remain valid
- Column names follow underscore_case convention (automatic via Hibernate)
- Multi-tenant isolation via `owner_id` column (@TenantId) already in place

**Rationale**: This is a code refactoring, not a schema change. Database remains stable, reducing deployment risk.

**Migration Strategy**:
- No Flyway migration scripts needed
- Database constraints remain unchanged
- Existing data requires no transformation
- Zero downtime deployment possible

**Alternatives Considered**:
- Rename tables: Rejected (breaks existing queries, adds migration complexity)
- Create new tables: Rejected (data duplication, migration risk)

---

### 5. Cross-Module API Design

**Decision**: Expose service interfaces for cross-module access

**Public API Surface**:

```kotlin
// UnitService.kt - Public interface for other modules
interface UnitService {
    fun findByUid(uid: String): UnitResponse?
    fun findByRefId(refId: String): UnitResponse?
    fun findAll(): List<UnitResponse>
    fun create(request: UnitRequest): UnitResponse
    fun update(uid: String, request: UnitRequest): UnitResponse
    fun delete(uid: String)

    // Usage validation
    fun isUnitInUse(uid: String): Boolean
    fun findProductsUsingUnit(uid: String): List<String>
}

// UnitConversionService.kt - Public interface
interface UnitConversionService {
    fun findByProductId(productId: String): List<UnitConversionResponse>
    fun convert(quantity: Double, fromUnitId: String, toUnitId: String, productId: String?): Double
    fun create(request: UnitConversionRequest): UnitConversionResponse
    fun update(uid: String, request: UnitConversionRequest): UnitConversionResponse
    fun delete(uid: String)
}
```

**Access Pattern**:
```kotlin
// In ProductService (or other modules)
@Service
class ProductService(
    private val unitService: UnitService,  // Inject from unit module
    private val productRepository: ProductRepository
) {
    fun createProduct(request: ProductRequest): ProductResponse {
        // Validate unit exists
        val unit = unitService.findByUid(request.unitId)
            ?: throw IllegalArgumentException("Unit not found")

        // ... rest of product creation
    }
}
```

**Rationale**: Service interfaces provide clean boundaries, enable mocking in tests, and allow module evolution without breaking consumers.

**Alternatives Considered**:
- Direct repository access: Rejected (violates encapsulation, exposes internal details)
- REST API calls between modules: Rejected (unnecessary network overhead, adds latency)

---

### 6. Testing Strategy

**Decision**: Three-tier testing approach

**Tier 1: Unit Tests** (fast, isolated)
```kotlin
@Test
fun `should create unit with valid data`() {
    val request = UnitRequest(...)
    val result = unitService.create(request)
    assertEquals(request.name, result.name)
}
```

**Tier 2: Integration Tests** (with H2 database)
```kotlin
@SpringBootTest
@Testcontainers  // or H2 in-memory
class UnitControllerIntegrationTest {
    @Test
    fun `should create and retrieve unit via API`() {
        val response = restTemplate.postForEntity("/api/v1/unit", request, UnitResponse::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode)
    }
}
```

**Tier 3: Cross-Module Integration Tests**
```kotlin
@SpringBootTest(classes = [AmpairsServiceApplication::class])
class UnitModuleIntegrationTest {
    @Test
    fun `product module should access unit module successfully`() {
        // Create unit via unit module
        val unit = unitService.create(unitRequest)

        // Use in product module
        val product = productService.create(
            ProductRequest(unitId = unit.uid, ...)
        )

        assertEquals(unit.uid, product.unitId)
    }
}
```

**Coverage Targets**:
- Service layer: ≥80% line coverage
- Controller layer: ≥90% endpoint coverage
- Repository layer: Covered by integration tests

**Rationale**: This approach catches issues at multiple levels: logic errors (unit tests), data access issues (integration tests), and cross-module compatibility (full integration tests).

**Alternatives Considered**:
- Unit tests only: Rejected (misses integration issues)
- Manual testing: Rejected (not repeatable, CI/CD incompatible)

---

### 7. Migration Execution Plan

**Decision**: Phased migration with validation gates

**Phase A: Create Unit Module Skeleton**
1. Create `ampairs-backend/unit/` directory
2. Add `build.gradle.kts` (copy from tax module, adjust names)
3. Update `settings.gradle.kts` to include unit module
4. Create package structure: `com.ampairs.unit.{config,controller,service,repository,domain,exception}`
5. Verify: `./gradlew :unit:build` succeeds (even with empty module)

**Phase B: Copy Unit Entities & DTOs**
1. Copy `Unit.kt` to `com.ampairs.unit.domain.model.Unit`
2. Copy `UnitConversion.kt` to `com.ampairs.unit.domain.model.UnitConversion`
3. Update package declarations and imports
4. Copy DTOs to `com.ampairs.unit.domain.dto.*`
5. Update DTO package references
6. Verify: `./gradlew :unit:build` succeeds

**Phase C: Copy Repositories & Services**
1. Copy `UnitRepository.kt` to `com.ampairs.unit.repository.UnitRepository`
2. Create `UnitService.kt` and `UnitServiceImpl.kt`
3. Create `UnitConversionService.kt` and `UnitConversionServiceImpl.kt`
4. Update all imports to reference new unit module
5. Verify: `./gradlew :unit:build` succeeds

**Phase D: Copy Controllers**
1. Create `UnitController.kt` with endpoints: GET /api/v1/unit, POST, PUT, DELETE
2. Create `UnitConversionController.kt` with conversion endpoints
3. Update imports and service injections
4. Verify: `./gradlew :unit:build` succeeds

**Phase E: Add Configuration**
1. Create `UnitModuleConfiguration.kt` for @Configuration and @ComponentScan
2. Create `Constants.kt` for UNIT_PREFIX and other constants
3. Verify: `./gradlew :unit:build` succeeds

**Phase F: Update Product Module**
1. Add `api(project(":unit"))` to product module's build.gradle.kts
2. Update product imports to reference `com.ampairs.unit.*` instead of local unit classes
3. Update ProductService to inject UnitService
4. Remove original unit classes from product module (do NOT delete entities yet - just deprecate)
5. Verify: `./gradlew :product:build` succeeds

**Phase G: Update Inventory & Invoice Modules**
1. Add unit module dependency if needed
2. Update imports to reference unit module
3. Refactor InventoryUnitConversion if necessary
4. Verify: `./gradlew :inventory:build` succeeds (inventory is subpackage of product)

**Phase H: Update Main Application**
1. Add `implementation(project(":unit"))` to ampairs_service/build.gradle.kts
2. Ensure component scanning includes unit module
3. Verify: `./gradlew :ampairs_service:bootRun` succeeds

**Phase I: Testing & Validation**
1. Run all unit tests: `./gradlew :unit:test`
2. Run all integration tests: `./gradlew :ampairs_service:test`
3. Manual API testing: Create unit → Create product with unit → Verify product retrieval
4. Verify multi-tenant isolation: Create unit in workspace A, verify not accessible from workspace B

**Phase J: Cleanup**
1. Remove deprecated unit classes from product module (if no longer referenced)
2. Update documentation (CLAUDE.md, module README if exists)
3. Code review and merge

**Validation Gates**:
- Each phase must have `./gradlew build` succeed before proceeding
- Integration tests must pass after Phase H
- API contract tests must pass before Phase J

**Rationale**: Phased approach allows incremental validation, easy rollback if issues arise, and clear progress tracking.

**Alternatives Considered**:
- Big-bang migration: Rejected (high risk, hard to debug)
- Feature flag approach: Rejected (unnecessary complexity for backend refactoring)

---

### 8. Risk Mitigation Strategies

**Risk 1: Circular dependency between unit and product modules**

**Mitigation**:
- Unit module depends only on core module
- Product module depends on unit module (one-way dependency)
- UnitConversion entity removed productId foreign key relationship, or keep as String reference only

**Risk 2: Breaking changes to existing API contracts**

**Mitigation**:
- API endpoints remain at same URLs: `/api/v1/unit`
- Response DTOs maintain same structure
- Add integration tests that verify API contract compatibility

**Risk 3: Multi-tenant isolation failures**

**Mitigation**:
- Unit entities already extend OwnableBaseDomain with @TenantId
- Add specific test cases for cross-tenant access validation
- Verify X-Workspace-ID header enforcement

**Risk 4: Test failures due to missing test data**

**Mitigation**:
- Copy existing test fixtures from product module
- Create dedicated unit test data builders
- Use H2 in-memory database for isolation

**Risk 5: Performance regression from N+1 queries**

**Mitigation**:
- Use @EntityGraph on UnitConversion relationships
- Add performance benchmarks before/after migration
- Monitor query counts in integration tests

---

## Summary of Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Module Structure | Follow tax module pattern exactly | Proven, constitution-compliant template |
| Dependency Direction | core ← unit ← product/inventory/invoice | Proper layering, clean boundaries |
| Database Schema | No changes; tables remain in place | Code refactoring only, reduces risk |
| API Design | Service interfaces for cross-module access | Encapsulation, testability, flexibility |
| Testing | Three-tier: unit, integration, cross-module | Comprehensive coverage at all levels |
| Migration | Phased with validation gates | Incremental progress, easy rollback |
| Build Config | Library module (bootJar disabled) | Same as tax module pattern |

## Next Steps

Proceed to **Phase 1: Design & Contracts** to create:
1. `data-model.md` - Entity and DTO specifications
2. `contracts/` - OpenAPI specifications for REST endpoints
3. `quickstart.md` - Developer guide for using the unit module

All research findings have been resolved. No NEEDS CLARIFICATION items remain.
