# Tasks: Multi-Timezone Support

**Input**: Design documents from `/specs/002-timezone-support/`
**Prerequisites**: plan.md ✅, research.md ✅
**Branch**: `002-timezone-support`
**Scope**: 34 entity files, 43+ controllers, Angular web app, KMP mobile app

## Summary

Migrate Ampairs platform from `LocalDateTime` (timezone-ambiguous) to `Instant` (UTC-based) across backend, frontend, and mobile. Store all timestamps in UTC, convert to user's local timezone on client side.

**Key Changes:**
- Backend: `LocalDateTime` → `Instant` in `BaseDomain` (affects 46+ entities)
- API: Add 'Z' suffix to ISO-8601 timestamps
- Frontend: Angular pipes for timezone-aware display
- Mobile: `kotlinx-datetime` for KMP timezone handling

**TDD Approach**: All tests must be written and must FAIL before implementation.

---

## Phase 3.1: Setup & Configuration

### T001 [P] Create feature branch and backup database
**File**: N/A (git operations)
**Description**:
- Create branch `002-timezone-support` from current branch
- Backup production database before any changes
- Document rollback procedure in `specs/002-timezone-support/ROLLBACK.md`
- Verify backup restoration works

**Test**: Can restore from backup successfully

---

### T002 [P] Add timezone configuration to application.yml
**File**: `/ampairs-backend/ampairs_service/src/main/resources/application.yml`
**Description**:
Add MySQL connection timezone parameter:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/munsi_app?serverTimezone=UTC
```

**Test**: Connection still works after adding parameter

---

### T003 [P] Create JacksonConfig for UTC timezone serialization
**File**: `/ampairs-backend/core/src/main/kotlin/com/ampairs/core/config/JacksonConfig.kt`
**Description**:
Create configuration class:
```kotlin
package com.ampairs.core.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

@Configuration
class JacksonConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder
                .timeZone(TimeZone.getTimeZone("UTC"))
                .modules(JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
```

**Test**: Creates new file, does not compile yet (no Instant fields to serialize)

---

### T004 [P] Set JVM default timezone to UTC in main application
**File**: `/ampairs-backend/ampairs_service/src/main/kotlin/com/ampairs/AmpairsApplication.kt`
**Description**:
Add `@PostConstruct` method to set timezone:
```kotlin
import jakarta.annotation.PostConstruct
import java.util.TimeZone

@PostConstruct
fun initTimezone() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    logger.info("JVM timezone set to UTC")
}
```

**Test**: Application starts successfully, logs "JVM timezone set to UTC"

---

## Phase 3.2: Tests First (TDD) ⚠️ CRITICAL - MUST FAIL BEFORE IMPLEMENTATION

### T005 [P] Create test utility for timezone verification
**File**: `/ampairs-backend/core/src/test/kotlin/com/ampairs/core/utils/TimezoneTestUtils.kt`
**Description**:
Create utility functions for testing:
```kotlin
package com.ampairs.core.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object TimezoneTestUtils {
    /**
     * Verify timestamp string ends with 'Z' (UTC indicator)
     */
    fun assertIsUtcTimestamp(timestamp: String) {
        assert(timestamp.endsWith("Z")) {
            "Timestamp must end with 'Z' for UTC: $timestamp"
        }
    }

    /**
     * Create Instant from epoch millis for testing
     */
    fun instantFromEpoch(epochMilli: Long): Instant =
        Instant.ofEpochMilli(epochMilli)

    /**
     * Verify two Instants are equal (accounting for precision)
     */
    fun assertInstantsEqual(expected: Instant, actual: Instant) {
        assert(expected.epochSecond == actual.epochSecond) {
            "Instants not equal: expected=$expected, actual=$actual"
        }
    }
}
```

**Test**: Utility compiles and basic assertions work

---

### T006 [P] Contract test: Instant serialization produces ISO-8601 with Z
**File**: `/ampairs-backend/core/src/test/kotlin/com/ampairs/core/serialization/InstantSerializationTest.kt`
**Description**:
Test that Instant fields serialize correctly:
```kotlin
package com.ampairs.core.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import com.ampairs.core.utils.TimezoneTestUtils.assertIsUtcTimestamp

@SpringBootTest
class InstantSerializationTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    data class TestEntity(
        val timestamp: Instant,
        val name: String
    )

    @Test
    fun `Instant should serialize to ISO-8601 with Z suffix`() {
        // Given
        val instant = Instant.parse("2025-01-09T14:30:00Z")
        val entity = TestEntity(instant, "test")

        // When
        val json = objectMapper.writeValueAsString(entity)

        // Then
        assert(json.contains("2025-01-09T14:30:00Z")) {
            "JSON should contain UTC timestamp with Z suffix: $json"
        }
        assert(!json.contains("timestamp\":")) {
            "JSON should use snake_case: $json"
        }
    }

    @Test
    fun `Instant should deserialize from ISO-8601 with Z suffix`() {
        // Given
        val json = """{"timestamp":"2025-01-09T14:30:00Z","name":"test"}"""

        // When
        val entity = objectMapper.readValue(json, TestEntity::class.java)

        // Then
        assert(entity.timestamp == Instant.parse("2025-01-09T14:30:00Z"))
    }
}
```

**Expected**: Test MUST FAIL (no Instant fields in entities yet)

---

### T007 [P] Integration test: BaseDomain with Instant timestamps
**File**: `/ampairs-backend/core/src/test/kotlin/com/ampairs/core/domain/BaseDomainInstantTest.kt`
**Description**:
Test that BaseDomain correctly handles Instant:
```kotlin
package com.ampairs.core.domain

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.temporal.ChronoUnit

@DataJpaTest
@ActiveProfiles("test")
class BaseDomainInstantTest {

    @Autowired
    private lateinit var entityManager: EntityManager

    // Test entity extending BaseDomain
    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "test_instant_entity")
    class TestInstantEntity : BaseDomain() {
        override fun obtainSeqIdPrefix(): String = "TEST"
        var name: String = ""
    }

    @Test
    fun `prePersist should set createdAt and updatedAt as Instant`() {
        // Given
        val entity = TestInstantEntity().apply {
            name = "test"
        }
        val beforeSave = Instant.now()

        // When
        entityManager.persist(entity)
        entityManager.flush()

        // Then
        assert(entity.createdAt != null) { "createdAt should be set" }
        assert(entity.updatedAt != null) { "updatedAt should be set" }
        assert(entity.createdAt is Instant) { "createdAt should be Instant" }
        assert(entity.updatedAt is Instant) { "updatedAt should be Instant" }

        // Verify timestamps are recent (within 5 seconds)
        val timeDiff = ChronoUnit.SECONDS.between(beforeSave, entity.createdAt!!)
        assert(timeDiff in 0..5) {
            "createdAt should be close to current time: diff=$timeDiff seconds"
        }
    }

    @Test
    fun `preUpdate should update updatedAt timestamp`() {
        // Given
        val entity = TestInstantEntity().apply { name = "test" }
        entityManager.persist(entity)
        entityManager.flush()
        val originalUpdatedAt = entity.updatedAt

        // Wait a moment to ensure timestamp changes
        Thread.sleep(1000)

        // When
        entity.name = "updated"
        entityManager.merge(entity)
        entityManager.flush()

        // Then
        assert(entity.updatedAt!! > originalUpdatedAt!!) {
            "updatedAt should be newer after update"
        }
    }
}
```

**Expected**: Test MUST FAIL (BaseDomain still uses LocalDateTime)

---

### T008 [P] Contract test: ApiResponse timestamp field with UTC
**File**: `/ampairs-backend/core/src/test/kotlin/com/ampairs/core/domain/dto/ApiResponseTimestampTest.kt`
**Description**:
Test that ApiResponse timestamp is UTC:
```kotlin
package com.ampairs.core.domain.dto

import com.ampairs.core.domain.dto.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ApiResponseTimestampTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `ApiResponse timestamp should be UTC with Z suffix`() {
        // Given
        val response = ApiResponse.success("test data")

        // When
        val json = objectMapper.writeValueAsString(response)

        // Then
        assert(json.contains("\"timestamp\":")) {
            "Should have timestamp field: $json"
        }
        assert(json.contains("Z\"")) {
            "Timestamp should end with Z: $json"
        }
        assert(json.matches(Regex(".*\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z\".*"))) {
            "Timestamp should be ISO-8601 with Z: $json"
        }
    }
}
```

**Expected**: Test MUST FAIL (ApiResponse.timestamp still LocalDateTime)

---

### T009 [P] Integration test: Workspace entity timestamps are UTC
**File**: `/ampairs-backend/workspace/src/test/kotlin/com/ampairs/workspace/model/WorkspaceTimezoneTest.kt`
**Description**:
Test that Workspace entity serializes with UTC:
```kotlin
package com.ampairs.workspace.model

import com.ampairs.workspace.repository.WorkspaceRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@Transactional
class WorkspaceTimezoneTest {

    @Autowired
    private lateinit var workspaceRepository: WorkspaceRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `Workspace createdAt should be Instant and serialize with Z`() {
        // Given
        val workspace = Workspace().apply {
            name = "Test Workspace"
            slug = "test-workspace"
            // createdAt/updatedAt set by @PrePersist
        }

        // When
        val saved = workspaceRepository.save(workspace)
        val json = objectMapper.writeValueAsString(saved)

        // Then
        assert(saved.createdAt is Instant) { "createdAt should be Instant" }
        assert(json.contains("\"created_at\":")) { "Should have created_at field" }
        assert(json.contains("Z\"")) { "Timestamp should end with Z: $json" }
    }
}
```

**Expected**: Test MUST FAIL (Workspace.createdAt still LocalDateTime via BaseDomain)

---

### T010 [P] E2E test: GET /workspace/v1 returns UTC timestamps
**File**: `/ampairs-backend/workspace/src/test/kotlin/com/ampairs/workspace/controller/WorkspaceControllerTimezoneTest.kt`
**Description**:
End-to-end test for workspace API timestamps:
```kotlin
package com.ampairs.workspace.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkspaceControllerTimezoneTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET workspace should return timestamps with Z suffix`() {
        // When
        val result = mockMvc.perform(
            get("/workspace/v1")
                .header("X-Workspace-ID", "test-workspace-id")
                .header("Authorization", "Bearer test-token")
        )

        // Then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.timestamp").value(endsWith("Z")))
            .andExpect(jsonPath("$.data[0].created_at").value(endsWith("Z")))
            .andExpect(jsonPath("$.data[0].updated_at").value(endsWith("Z")))
    }
}
```

**Expected**: Test MUST FAIL (timestamps don't have Z suffix yet)

---

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### T011 Update BaseDomain to use Instant instead of LocalDateTime
**File**: `/ampairs-backend/core/src/main/kotlin/com/ampairs/core/domain/model/BaseDomain.kt`
**Description**:
Change `LocalDateTime` to `Instant`:
```kotlin
// BEFORE:
import java.time.LocalDateTime
var createdAt: LocalDateTime? = null
var updatedAt: LocalDateTime? = null
val now = LocalDateTime.now()

// AFTER:
import java.time.Instant
var createdAt: Instant? = null
var updatedAt: Instant? = null
val now = Instant.now()
```

**Dependencies**: After T006, T007 fail
**Test**: T007 should now PASS

---

### T012 Update ApiResponse to use Instant for timestamp field
**File**: `/ampairs-backend/core/src/main/kotlin/com/ampairs/core/domain/dto/ApiResponse.kt`
**Description**:
Change timestamp field from `LocalDateTime` to `Instant`:
```kotlin
// BEFORE:
val timestamp: LocalDateTime = LocalDateTime.now()

// AFTER:
val timestamp: Instant = Instant.now()
```

**Dependencies**: After T008 fails
**Test**: T008 should now PASS

---

### T013 [P] Create TimeUtils utility for timezone operations
**File**: `/ampairs-backend/core/src/main/kotlin/com/ampairs/core/utils/TimeUtils.kt`
**Description**:
Create utility functions for common timezone operations:
```kotlin
package com.ampairs.core.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeUtils {

    /**
     * Get current time as Instant (UTC)
     */
    fun now(): Instant = Instant.now()

    /**
     * Convert Instant to LocalDateTime in given timezone
     */
    fun toLocalDateTime(instant: Instant, zoneId: ZoneId = ZoneId.of("UTC")): LocalDateTime {
        return LocalDateTime.ofInstant(instant, zoneId)
    }

    /**
     * Convert LocalDateTime (assumed UTC) to Instant
     */
    fun toInstant(localDateTime: LocalDateTime): Instant {
        return localDateTime.toInstant(ZoneOffset.UTC)
    }

    /**
     * Format Instant as ISO-8601 string with Z
     */
    fun formatIso8601(instant: Instant): String {
        return instant.toString() // Already in ISO-8601 format with Z
    }

    /**
     * Parse ISO-8601 string to Instant
     */
    fun parseIso8601(isoString: String): Instant {
        return Instant.parse(isoString)
    }
}
```

**Test**: Unit tests pass for each utility function

---

### T014 [P] Fix compilation errors in all entity classes
**File**: All entity classes in `/ampairs-backend/*/src/main/kotlin/com/ampairs/*/domain/model/*.kt`
**Description**:
Fix any compilation errors caused by BaseDomain change. Most entities should compile without changes since they inherit from BaseDomain. Check for:
- Direct usage of `LocalDateTime` in entity fields (outside BaseDomain)
- Custom `@PrePersist` / `@PreUpdate` methods using LocalDateTime
- DTO mapping logic that expects LocalDateTime

**Estimated affected files**: 34 entity files
**Method**: Compile entire project, fix errors one by one

**Test**: `./gradlew build` succeeds for all modules

---

### T015 [P] Update DTO response classes with Instant fields
**File**: All DTO classes in `/ampairs-backend/*/src/main/kotlin/com/ampairs/*/domain/dto/*Response.kt`
**Description**:
Update response DTOs that expose timestamp fields:
- Change `LocalDateTime` → `Instant` in DTO definitions
- Update extension functions `entity.asEntityResponse()` to handle Instant

Example:
```kotlin
// BEFORE:
data class WorkspaceResponse(
    val uid: String,
    val name: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

// AFTER:
data class WorkspaceResponse(
    val uid: String,
    val name: String,
    val createdAt: Instant?,
    val updatedAt: Instant?
)
```

**Test**: All response DTOs compile and serialize correctly

---

### T016 Verify T006 contract test now passes
**File**: `/ampairs-backend/core/src/test/kotlin/com/ampairs/core/serialization/InstantSerializationTest.kt`
**Description**: Run T006 test and verify it passes

**Expected**: GREEN - Test passes showing Instant serializes to ISO-8601 with Z

---

### T017 Verify T009 integration test now passes
**File**: `/ampairs-backend/workspace/src/test/kotlin/com/ampairs/workspace/model/WorkspaceTimezoneTest.kt`
**Description**: Run T009 test and verify it passes

**Expected**: GREEN - Workspace entity uses Instant and serializes with Z

---

### T018 Verify T010 E2E test now passes
**File**: `/ampairs-backend/workspace/src/test/kotlin/com/ampairs/workspace/controller/WorkspaceControllerTimezoneTest.kt`
**Description**: Run T010 test and verify it passes

**Expected**: GREEN - API responses include Z suffix in timestamps

---

## Phase 3.4: Database Migration

### T019 Create database audit script for existing timestamps
**File**: `/ampairs-backend/ampairs_service/src/main/resources/db/audit/audit_timestamps.sql`
**Description**:
Create SQL script to audit current timestamp data:
```sql
-- Check sample timestamps from key tables
SELECT
    'workspace' as table_name,
    COUNT(*) as row_count,
    MIN(created_at) as earliest_timestamp,
    MAX(created_at) as latest_timestamp,
    MIN(updated_at) as earliest_updated,
    MAX(updated_at) as latest_updated
FROM workspace
UNION ALL
SELECT
    'customer' as table_name,
    COUNT(*) as row_count,
    MIN(created_at) as earliest_timestamp,
    MAX(created_at) as latest_timestamp,
    MIN(updated_at) as earliest_updated,
    MAX(updated_at) as latest_updated
FROM customer
UNION ALL
SELECT
    'product' as table_name,
    COUNT(*) as row_count,
    MIN(created_at) as earliest_timestamp,
    MAX(created_at) as latest_timestamp,
    MIN(updated_at) as earliest_updated,
    MAX(updated_at) as latest_updated
FROM product;

-- Check for any NULL timestamps (should not exist)
SELECT 'workspace' as table_name, uid
FROM workspace
WHERE created_at IS NULL OR updated_at IS NULL
UNION ALL
SELECT 'customer' as table_name, uid
FROM customer
WHERE created_at IS NULL OR updated_at IS NULL;
```

**Test**: Script runs and returns results showing current data state

---

### T020 Document current timezone assumptions
**File**: `/specs/002-timezone-support/MIGRATION_NOTES.md`
**Description**:
Document findings from T019:
- Current server timezone (check with `SELECT @@system_time_zone`)
- Range of timestamps in database
- Any anomalies or NULL values
- Assumption about timezone of existing data (likely server local time)

**Test**: Document is complete and reviewed

---

### T021 Create database migration script (DRY RUN)
**File**: `/ampairs-backend/ampairs_service/src/main/resources/db/migration/V2_0__migrate_timestamps_to_utc_dryrun.sql`
**Description**:
Create DRY RUN script that shows what would change:
```sql
-- DRY RUN: This script only SELECTS, does not UPDATE
-- Purpose: Verify migration logic before applying

-- Show what would be updated for workspace table
SELECT
    uid,
    created_at as current_created_at,
    -- MySQL TIMESTAMP columns already store UTC
    -- This migration is primarily for documentation
    created_at as new_created_at_utc,
    'No change needed - TIMESTAMP already UTC' as notes
FROM workspace
LIMIT 10;

-- Check all tables with timestamps
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND COLUMN_NAME IN ('created_at', 'updated_at')
ORDER BY TABLE_NAME, COLUMN_NAME;
```

**Test**: Script runs and shows current state without modifications

---

### T022 Verify MySQL TIMESTAMP columns are already UTC
**File**: N/A (verification step)
**Description**:
Verify that MySQL `TIMESTAMP` columns already store UTC:
1. Check table definitions: `SHOW CREATE TABLE workspace;`
2. Verify columns are `TIMESTAMP` not `DATETIME`
3. Confirm `serverTimezone=UTC` in connection string (from T002)

**Expected Finding**: TIMESTAMP columns already store UTC internally, no data migration needed

**Document in**: MIGRATION_NOTES.md

---

### T023 Create backup and restore procedure
**File**: `/specs/002-timezone-support/BACKUP_RESTORE.md`
**Description**:
Document backup and restore procedures:
```bash
# Backup
mysqldump -u root -p munsi_app > backup_before_timezone_migration.sql

# Verify backup
mysql -u root -p munsi_app < backup_before_timezone_migration.sql --dry-run

# Restore if needed
mysql -u root -p munsi_app < backup_before_timezone_migration.sql
```

**Test**: Backup and restore works on test database

---

## Phase 3.5: Integration Testing

### T024 [P] Add timezone integration test for Customer module
**File**: `/ampairs-backend/customer/src/test/kotlin/com/ampairs/customer/CustomerTimezoneIntegrationTest.kt`
**Description**:
Integration test verifying customer timestamps are UTC:
```kotlin
@SpringBootTest
@Transactional
class CustomerTimezoneIntegrationTest {

    @Autowired
    private lateinit var customerService: CustomerService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `Customer creation should use UTC timestamps`() {
        // Given
        val customer = Customer().apply {
            name = "Test Customer"
            // timestamps set automatically
        }

        // When
        val saved = customerService.save(customer)
        val json = objectMapper.writeValueAsString(saved)

        // Then
        assert(saved.createdAt is Instant)
        assert(json.contains("Z\"")) { "Should have UTC timestamps: $json" }
    }
}
```

**Test**: Integration test passes

---

### T025 [P] Add timezone integration test for Product module
**File**: `/ampairs-backend/product/src/test/kotlin/com/ampairs/product/ProductTimezoneIntegrationTest.kt`
**Description**: Similar to T024, for Product entity

**Test**: Integration test passes

---

### T026 [P] Add timezone integration test for Order module
**File**: `/ampairs-backend/order/src/test/kotlin/com/ampairs/order/OrderTimezoneIntegrationTest.kt`
**Description**: Similar to T024, for Order entity

**Test**: Integration test passes

---

### T027 [P] Add timezone integration test for Invoice module
**File**: `/ampairs-backend/invoice/src/test/kotlin/com/ampairs/invoice/InvoiceTimezoneIntegrationTest.kt`
**Description**: Similar to T024, for Invoice entity

**Test**: Integration test passes

---

### T028 Run full backend test suite and fix any failures
**File**: N/A (test execution)
**Description**:
Run all backend tests: `./gradlew test`

Fix any test failures related to:
- Test assertions expecting LocalDateTime
- Test data builders using LocalDateTime
- Mock data with timestamp comparisons

**Test**: All backend tests pass (green)

---

## Phase 3.6: Frontend Implementation

### T029 [P] Create Angular date formatting pipe for local timezone
**File**: `/ampairs-web/src/app/core/pipes/local-date.pipe.ts`
**Description**:
Create pipe to format UTC timestamps in user's timezone:
```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'localDate',
  standalone: true
})
export class LocalDatePipe implements PipeTransform {
  transform(value: string | Date | null, format: string = 'medium'): string {
    if (!value) return '';

    const date = typeof value === 'string' ? new Date(value) : value;

    // Angular DatePipe automatically uses browser timezone
    return new Intl.DateTimeFormat('default', {
      dateStyle: format.includes('date') ? 'medium' : undefined,
      timeStyle: format.includes('time') ? 'medium' : undefined
    }).format(date);
  }
}
```

**Test**: Pipe correctly formats ISO-8601 with Z suffix

---

### T030 [P] Create Angular relative time pipe ("2 hours ago")
**File**: `/ampairs-web/src/app/core/pipes/relative-time.pipe.ts`
**Description**:
Create pipe for relative time display:
```typescript
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'relativeTime',
  standalone: true
})
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | Date | null): string {
    if (!value) return '';

    const date = typeof value === 'string' ? new Date(value) : value;
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSeconds < 60) return 'just now';
    if (diffMinutes < 60) return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;

    return date.toLocaleDateString();
  }
}
```

**Test**: Pipe calculates relative time correctly

---

### T031 [P] Create date utilities for parsing and formatting
**File**: `/ampairs-web/src/app/shared/utils/date-utils.ts`
**Description**:
Create utility functions:
```typescript
export class DateUtils {
  /**
   * Parse ISO-8601 UTC string to Date object
   */
  static parseUtc(isoString: string): Date {
    return new Date(isoString);
  }

  /**
   * Format Date as ISO-8601 UTC string for API
   */
  static toUtcString(date: Date): string {
    return date.toISOString();
  }

  /**
   * Check if string is valid ISO-8601 with Z
   */
  static isUtcTimestamp(value: string): boolean {
    return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z$/.test(value);
  }
}
```

**Test**: Utility functions work correctly

---

### T032 Update workspace components to use new date pipes
**File**: `/ampairs-web/src/app/pages/workspace/workspace-select/workspace-select.component.html` (and others)
**Description**:
Update template to use new pipes:
```html
<!-- BEFORE -->
<span>{{ workspace.created_at }}</span>

<!-- AFTER -->
<span>{{ workspace.created_at | localDate:'medium' }}</span>
<span class="text-muted">{{ workspace.created_at | relativeTime }}</span>
```

**Test**: Timestamps display in browser's local timezone

---

### T033 [P] Add Cypress E2E test for timezone display
**File**: `/ampairs-web/cypress/e2e/timezone/workspace-timezone.cy.ts`
**Description**:
E2E test verifying timezone display:
```typescript
describe('Workspace Timezone Display', () => {
  it('should display timestamps in local timezone', () => {
    cy.login();
    cy.visit('/workspaces');

    // Intercept API call
    cy.intercept('GET', '/workspace/v1').as('getWorkspaces');

    cy.wait('@getWorkspaces').then((interception) => {
      // Verify API returns UTC timestamps
      expect(interception.response.body.data[0].created_at).to.match(/Z$/);

      // Verify UI displays formatted timestamp (not raw UTC)
      cy.get('[data-cy=workspace-card]').first().within(() => {
        cy.get('[data-cy=created-date]').should('not.contain', 'Z');
        cy.get('[data-cy=created-date]').should('match', /\d{1,2}:\d{2}/);
      });
    });
  });
});
```

**Test**: E2E test passes

---

### T034 Update all Angular components displaying timestamps
**File**: Multiple files in `/ampairs-web/src/app/pages/**/*.html`
**Description**:
Search for all timestamp displays and update to use pipes:
- Search: `{{ *.created_at }}`, `{{ *.updated_at }}`, `{{ *.timestamp }}`
- Update: Add `| localDate` or `| relativeTime` pipe

**Estimate**: 20-30 template files

**Test**: `ng build` succeeds, manual verification of timestamp display

---

## Phase 3.7: Mobile Implementation (Kotlin Multiplatform)

### T035 Add kotlinx-datetime dependency to KMP project
**File**: `/ampairs-mp-app/composeApp/build.gradle.kts`
**Description**:
Add dependency:
```kotlin
commonMain.dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
}
```

**Test**: Project builds successfully with new dependency

---

### T036 [P] Create DateTimeUtils for KMP
**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/core/utils/DateTimeUtils.kt`
**Description**:
Create utility for timezone handling:
```kotlin
package com.ampairs.core.utils

import kotlinx.datetime.*

object DateTimeUtils {
    /**
     * Get current timezone
     */
    fun currentTimeZone(): TimeZone = TimeZone.currentSystemDefault()

    /**
     * Parse ISO-8601 UTC string to Instant
     */
    fun parseUtc(isoString: String): Instant {
        return Instant.parse(isoString)
    }

    /**
     * Format Instant in local timezone
     */
    fun formatLocal(instant: Instant, timeZone: TimeZone = currentTimeZone()): String {
        val localDateTime = instant.toLocalDateTime(timeZone)
        return "${localDateTime.date} ${localDateTime.time}"
    }

    /**
     * Format as relative time ("2 hours ago")
     */
    fun formatRelative(instant: Instant): String {
        val now = Clock.System.now()
        val diff = now - instant

        return when {
            diff.inWholeSeconds < 60 -> "just now"
            diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes} minutes ago"
            diff.inWholeHours < 24 -> "${diff.inWholeHours} hours ago"
            diff.inWholeDays < 7 -> "${diff.inWholeDays} days ago"
            else -> formatLocal(instant)
        }
    }
}
```

**Test**: Utils compile and work on all platforms (Android, iOS, Desktop)

---

### T037 Update Workspace data classes to use kotlinx.datetime.Instant
**File**: `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/workspace/data/Workspace.kt`
**Description**:
Update data class:
```kotlin
import kotlinx.datetime.Instant

data class Workspace(
    val uid: String,
    val name: String,
    val createdAt: Instant?,  // Changed from String or LocalDateTime
    val updatedAt: Instant?
)
```

**Test**: Builds successfully, JSON serialization works

---

### T038 Update mobile UI to format timestamps in local timezone
**File**: Multiple files in `/ampairs-mp-app/composeApp/src/commonMain/kotlin/com/ampairs/*/ui/**/*.kt`
**Description**:
Update Composable functions to format timestamps:
```kotlin
// BEFORE
Text(workspace.createdAt.toString())

// AFTER
Text(DateTimeUtils.formatLocal(workspace.createdAt))
Text(
    text = DateTimeUtils.formatRelative(workspace.createdAt),
    style = MaterialTheme.typography.bodySmall
)
```

**Test**: UI displays timestamps in device's local timezone

---

### T039 [P] Add mobile unit tests for timezone utilities
**File**: `/ampairs-mp-app/composeApp/src/commonTest/kotlin/com/ampairs/core/utils/DateTimeUtilsTest.kt`
**Description**:
Unit tests for DateTimeUtils:
```kotlin
class DateTimeUtilsTest {
    @Test
    fun testParseUtc() {
        val instant = DateTimeUtils.parseUtc("2025-01-09T14:30:00Z")
        assertEquals(1736432400, instant.epochSeconds)
    }

    @Test
    fun testFormatRelative() {
        val now = Clock.System.now()
        val twoHoursAgo = now - 2.hours
        val result = DateTimeUtils.formatRelative(twoHoursAgo)
        assertTrue(result.contains("hours ago"))
    }
}
```

**Test**: Unit tests pass on all platforms

---

## Phase 3.8: Performance & Validation

### T040 [P] Create performance benchmark for Instant vs LocalDateTime
**File**: `/ampairs-backend/core/src/test/kotlin/com/ampairs/core/performance/InstantPerformanceTest.kt`
**Description**:
Benchmark serialization performance:
```kotlin
@Test
fun `benchmark Instant serialization performance`() {
    val iterations = 10000
    val instant = Instant.now()
    val entity = TestEntity(instant, "test")

    val startTime = System.currentTimeMillis()
    repeat(iterations) {
        objectMapper.writeValueAsString(entity)
    }
    val endTime = System.currentTimeMillis()

    val avgTimeMs = (endTime - startTime).toDouble() / iterations

    // Assert less than 10ms per operation
    assert(avgTimeMs < 10.0) {
        "Serialization too slow: $avgTimeMs ms per operation"
    }

    println("Instant serialization: $avgTimeMs ms per operation")
}
```

**Expected**: <10ms per operation (success criteria from plan)

---

### T041 Run full test suite (backend, web, mobile)
**File**: N/A (test execution)
**Description**:
Run all tests across all projects:
```bash
# Backend
cd ampairs-backend && ./gradlew test

# Web
cd ampairs-web && npm test

# Mobile
cd ampairs-mp-app && ./gradlew test
```

**Test**: All tests pass (GREEN)

---

### T042 Manual testing: Verify timestamps in different timezones
**File**: `/specs/002-timezone-support/MANUAL_TESTING.md`
**Description**:
Manual test checklist:
1. Change system timezone to EST, IST, PST
2. Open web app in each timezone
3. Verify timestamps display correctly in local time
4. Create new entities, verify they save with UTC timestamps
5. Test on physical iOS device in different timezone
6. Test on physical Android device in different timezone

**Test**: Timestamps display correctly across all timezones

---

### T043 Verify no data loss from migration
**File**: N/A (verification step)
**Description**:
1. Run audit script from T019 again
2. Compare row counts before/after
3. Verify no NULL timestamps introduced
4. Spot-check timestamp values are reasonable

**Test**: No data loss, all timestamps valid

---

## Phase 3.9: Documentation & Cleanup

### T044 [P] Update CLAUDE.md with timezone guidelines
**File**: `/ampairs-backend/CLAUDE.md`
**Description**:
Add new section after "Recent Changes":
```markdown
### **Timezone Handling (2025-01-09)**

#### **UTC Storage Standard**
- All timestamps stored as `java.time.Instant` (UTC)
- Database uses MySQL `TIMESTAMP` columns (UTC-aware)
- API responses include ISO-8601 format with 'Z' suffix

#### **Entity Pattern**
```kotlin
import java.time.Instant

@Entity
class MyEntity : BaseDomain() {
    // createdAt/updatedAt inherited as Instant

    // For custom timestamp fields, use Instant
    @Column(name = "scheduled_at")
    var scheduledAt: Instant? = null
}
```

#### **DTO Pattern**
```kotlin
data class MyResponse(
    val uid: String,
    val createdAt: Instant?,  // Serializes to "2025-01-09T14:30:00Z"
    val updatedAt: Instant?
)
```

#### **Client-Side Conversion**
- **Web**: Use Angular pipes `| localDate` or `| relativeTime`
- **Mobile**: Use `DateTimeUtils.formatLocal()` or `formatRelative()`
- **Never** use `LocalDateTime` for historical timestamps

#### **Testing**
- All timestamp tests verify ISO-8601 with Z suffix
- Integration tests check UTC storage in database
- E2E tests verify client-side timezone conversion
```

**Test**: Documentation is clear and accurate

---

### T045 [P] Create migration guide for API clients
**File**: `/specs/002-timezone-support/API_MIGRATION_GUIDE.md`
**Description**:
Document for external API consumers:
```markdown
# API Timezone Migration Guide

## Breaking Change: Timestamp Format

**Effective Date**: Version 2.0.0

### What Changed

**Before (v1.x):**
```json
{
  "created_at": "2025-01-09T14:30:00"
}
```

**After (v2.0+):**
```json
{
  "created_at": "2025-01-09T14:30:00Z"
}
```

### Impact

All timestamp fields now include 'Z' suffix indicating UTC.

### Migration Steps

1. Update timestamp parsing to handle ISO-8601 with Z
2. Convert to local timezone for display (client-side)
3. Send timestamps in ISO-8601 UTC format

### Code Examples

[Include examples for common languages: JavaScript, Python, Java, etc.]
```

**Test**: Guide is complete and understandable

---

### T046 [P] Add logging for timezone-related errors
**File**: `/ampairs-backend/core/src/main/kotlin/com/ampairs/core/exception/TimezoneExceptionHandler.kt`
**Description**:
Add exception handler for timezone conversion errors:
```kotlin
package com.ampairs.core.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.format.DateTimeParseException

@RestControllerAdvice
class TimezoneExceptionHandler {

    @ExceptionHandler(DateTimeParseException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDateTimeParseException(ex: DateTimeParseException): ApiResponse<Any> {
        logger.error("Invalid timestamp format: ${ex.message}", ex)
        return ApiResponse.error(
            message = "Invalid timestamp format. Expected ISO-8601 with Z suffix (e.g., 2025-01-09T14:30:00Z)",
            errorDetails = ErrorDetails(
                code = "INVALID_TIMESTAMP_FORMAT",
                field = "timestamp",
                message = ex.message
            )
        )
    }
}
```

**Test**: Invalid timestamp formats return clear error messages

---

### T047 Remove any remaining LocalDateTime usage
**File**: Multiple files across backend
**Description**:
Search codebase for remaining `LocalDateTime` usage:
```bash
grep -r "LocalDateTime" --include="*.kt" ampairs-backend/
```

For each occurrence:
- If it's a timestamp: Replace with `Instant`
- If it's a scheduled future event: Keep as `LocalDateTime` but document timezone separately
- If it's in test data: Update to use `Instant`

**Test**: No inappropriate `LocalDateTime` usage remains

---

### T048 Update API documentation (OpenAPI/Swagger)
**File**: Swagger annotations in controller classes
**Description**:
Add examples to Swagger docs showing UTC format:
```kotlin
@Operation(summary = "Get workspace details")
@ApiResponse(
    responseCode = "200",
    description = "Success",
    content = [Content(
        examples = [ExampleObject(
            value = """
            {
              "success": true,
              "data": {
                "uid": "WS-123456",
                "name": "Acme Corp",
                "created_at": "2025-01-09T14:30:00Z",
                "updated_at": "2025-01-09T15:45:00Z"
              },
              "timestamp": "2025-01-09T16:00:00Z"
            }
            """
        )]
    )]
)
```

**Test**: Swagger UI shows correct timestamp format in examples

---

## Phase 3.10: Deployment & Rollout

### T049 Create deployment checklist
**File**: `/specs/002-timezone-support/DEPLOYMENT_CHECKLIST.md`
**Description**:
Checklist for production deployment:
```markdown
# Deployment Checklist

## Pre-Deployment
- [ ] All tests passing (backend, web, mobile)
- [ ] Database backup completed and verified
- [ ] Rollback procedure documented and tested
- [ ] API migration guide published
- [ ] Performance benchmarks meet criteria (<10ms)

## Deployment Steps
1. [ ] Deploy backend (zero-downtime deployment)
2. [ ] Verify backend health checks
3. [ ] Deploy web frontend
4. [ ] Deploy mobile app (gradual rollout)
5. [ ] Monitor error logs for timezone issues
6. [ ] Verify API responses include Z suffix
7. [ ] Check database for any NULL timestamps

## Post-Deployment
- [ ] Run smoke tests in production
- [ ] Monitor performance metrics (24 hours)
- [ ] Check user reports for timezone issues
- [ ] Verify no data loss (compare row counts)
- [ ] Update version number to 2.0.0

## Rollback Trigger
If any of these occur:
- More than 1% error rate increase
- Data loss detected
- Performance degradation >20%
- Critical user-facing timezone bugs
```

**Test**: Checklist is comprehensive

---

### T050 Final verification: All success criteria met
**File**: `/specs/002-timezone-support/SUCCESS_VERIFICATION.md`
**Description**:
Verify all success criteria from plan.md:

1. ✅ All backend timestamps stored as UTC in database
2. ✅ All API responses include 'Z' suffix in timestamp fields
3. ✅ Frontend displays dates in user's browser timezone
4. ✅ Mobile apps display dates in device's timezone
5. ✅ No data loss during migration
6. ✅ All tests passing (contract, integration, E2E)
7. ✅ Performance: <10ms overhead for timezone conversion
8. ✅ Zero production incidents related to timezone issues (monitor)

**Test**: All criteria verified and documented

---

## Dependencies

### Critical Path
```
Setup (T001-T004)
  → Tests Written (T005-T010) MUST FAIL
    → Core Implementation (T011-T015) MAKE TESTS PASS
      → Test Verification (T016-T018)
        → Database Migration (T019-T023)
          → Integration Tests (T024-T028)
            → Frontend (T029-T034) [parallel with Mobile T035-T039]
              → Performance (T040-T043)
                → Documentation (T044-T048)
                  → Deployment (T049-T050)
```

### Parallel Execution Opportunities

**Phase 3.1 Setup** (all parallel):
- T001, T002, T003, T004

**Phase 3.2 Tests** (all parallel):
- T005, T006, T007, T008, T009, T010

**Phase 3.3 Implementation** (some parallel):
- T013 (TimeUtils) parallel with T011-T012
- T014, T015 after T011

**Phase 3.5 Integration Tests** (all parallel):
- T024, T025, T026, T027

**Phase 3.6 + 3.7** (entire phases parallel):
- Frontend T029-T034
- Mobile T035-T039

**Phase 3.9 Documentation** (all parallel):
- T044, T045, T046

---

## Parallel Execution Example

Launch all setup tasks together:
```bash
# Terminal 1
git checkout -b 002-timezone-support
mysqldump -u root -p munsi_app > backup.sql

# Terminal 2 (or use Task agents)
# Edit application.yml
# Create JacksonConfig.kt
# Update AmpairsApplication.kt
```

Launch all test creation tasks together:
```bash
# Use Task agent to create all test files in parallel:
Task: "Create TimezoneTestUtils.kt with utility functions"
Task: "Create InstantSerializationTest.kt contract test"
Task: "Create BaseDomainInstantTest.kt integration test"
Task: "Create ApiResponseTimestampTest.kt contract test"
Task: "Create WorkspaceTimezoneTest.kt integration test"
Task: "Create WorkspaceControllerTimezoneTest.kt E2E test"
```

---

## Notes

- **TDD Enforcement**: Tests in Phase 3.2 MUST be written and MUST FAIL before any implementation
- **Zero Data Loss**: MySQL TIMESTAMP columns already store UTC, so no actual data migration needed
- **Backward Compatibility**: Old clients may need update, provide API migration guide (T045)
- **Performance**: Monitor serialization overhead, should be <10ms per operation (T040)
- **Gradual Rollout**: Mobile apps can be deployed gradually to monitor for issues

---

## Validation Checklist

- [x] All tests come before implementation (T005-T010 before T011-T018)
- [x] Parallel tasks truly independent (different files, no dependencies)
- [x] Each task specifies exact file path
- [x] Core entity (BaseDomain) changed before dependent entities
- [x] Integration tests cover all major modules
- [x] Frontend and mobile can be done in parallel
- [x] Documentation completed before deployment
- [x] Rollback procedure documented

---

## Estimated Timeline

- Phase 3.1 Setup: 2 hours
- Phase 3.2 Tests: 4 hours
- Phase 3.3 Implementation: 6 hours
- Phase 3.4 Database: 3 hours
- Phase 3.5 Integration: 4 hours
- Phase 3.6 Frontend: 6 hours
- Phase 3.7 Mobile: 6 hours
- Phase 3.8 Performance: 2 hours
- Phase 3.9 Documentation: 3 hours
- Phase 3.10 Deployment: 2 hours

**Total**: ~38 hours (~1 week for single developer, ~3 days with parallel execution)

---

*Tasks generated: 2025-01-09*
*Ready for execution following TDD principles*
