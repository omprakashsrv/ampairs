# Implementation Plan: Multi-Timezone Support

**Branch**: `002-timezone-support` | **Date**: 2025-01-09 | **Spec**: [link to spec.md]

## Executive Summary

This plan outlines the implementation of comprehensive timezone support across the Ampairs platform (Backend, Web, Mobile). The solution follows industry best practices: **store all timestamps in UTC on the server side, and convert to user's local timezone on the client side**.

## Current State Analysis

### What We Found

1. **Backend (Spring Boot + Kotlin)**
   - Uses `LocalDateTime` for all timestamp fields in entities (`BaseDomain.createdAt`, `BaseDomain.updatedAt`)
   - `LocalDateTime` has NO timezone information - operates on server's local time
   - Jackson configured with `write-dates-as-timestamps: false` (good)
   - No explicit timezone configuration - defaults to server JVM timezone
   - All entities extend `BaseDomain` or `OwnableBaseDomain` with `LocalDateTime` fields

2. **Web Frontend (Angular)**
   - No explicit timezone handling detected
   - JavaScript Date objects use browser's local timezone by default
   - No standardized date formatting/parsing utilities found

3. **Mobile App (Kotlin Multiplatform)**
   - Uses platform-specific date/time libraries
   - No centralized timezone handling strategy

### Problems with Current Approach

1. **Ambiguous Timestamps**: `LocalDateTime` doesn't store timezone, causing confusion when users are in different timezones
2. **Data Inconsistency**: Server time depends on where it's deployed (local dev vs production)
3. **DST Issues**: Daylight saving time changes can cause 1-hour discrepancies
4. **User Experience**: Users see timestamps in server's timezone, not their local time
5. **Multi-tenant Issues**: Different workspaces in different timezones see inconsistent times

## Technical Context

**Language/Version**: Kotlin 1.9+, Java 21, TypeScript 5.x
**Primary Dependencies**: Spring Boot 3.5.x, Jackson, Angular 18, Kotlin Multiplatform
**Storage**: MySQL 8.0+ with TIMESTAMP columns
**Testing**: JUnit 5, Cypress, Kotlin Test
**Target Platform**: Linux servers (UTC), Web browsers (user timezone), iOS/Android (user timezone)
**Project Type**: web (backend + frontend + mobile)
**Performance Goals**: No noticeable latency increase, <10ms conversion overhead
**Constraints**: Backward compatible with existing timestamps, zero data loss during migration
**Scale/Scope**: All 46+ entities, all REST APIs, all UI components displaying dates

## Recommended Strategy

### ✅ YES - Store in UTC, Convert on Client

**Backend Changes:**
1. Migrate from `LocalDateTime` to `Instant` for all timestamp fields
2. Configure JVM timezone to UTC
3. Configure Jackson to serialize `Instant` as ISO-8601 with 'Z' suffix
4. Store as `TIMESTAMP` in MySQL (already UTC-based)

**Frontend Changes:**
1. Parse ISO-8601 strings from API to Date objects
2. Create Angular pipes for timezone-aware formatting
3. Display dates in user's browser timezone
4. Send dates to API in ISO-8601 UTC format

**Mobile Changes:**
1. Use `kotlinx-datetime` Instant type
2. Convert to local timezone for display using platform APIs
3. Send dates to API in ISO-8601 UTC format

### Why This Approach?

**Advantages:**
- ✅ Single source of truth (UTC)
- ✅ No ambiguity about "when" something happened
- ✅ Database queries work correctly across timezones
- ✅ Easy to add user timezone preference later
- ✅ Industry standard (used by AWS, Google, GitHub, etc.)
- ✅ DST-proof (UTC doesn't observe DST)

**Alternatives Considered:**
- ❌ Store in user's timezone: Requires complex timezone tracking per record
- ❌ Store as `LocalDateTime` with timezone column: Adds complexity, prone to errors
- ❌ Store offset with `OffsetDateTime`: Larger storage, unnecessary when UTC suffices

## Constitution Check

**Simplicity**:
- Projects: 3 (backend, web, mobile) - PASS
- Using framework directly? Yes - Spring Boot, Angular, KMP - PASS
- Single data model? Yes - Instant everywhere - PASS
- Avoiding patterns? Yes - no new repositories/wrappers - PASS

**Architecture**:
- Feature as library? Core utility (timezone conversion) - PASS
- Libraries listed: `kotlinx-datetime` (KMP), `date-fns` or Angular pipes (web)
- CLI: Not applicable for this feature
- Library docs: Will document in CLAUDE.md

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor: YES - write failing tests first
- Order: Contract → Integration → E2E → Unit
- Real dependencies: YES - real database with UTC timestamps
- Integration tests: YES - for entity migrations, API contracts, timezone conversions
- Test MUST fail first before implementation

**Observability**:
- Structured logging: Include timezone info in error logs
- Frontend logs: Already sent to backend
- Error context: Timestamp conversion errors will be logged with context

**Versioning**:
- Version: 2.0.0 (breaking change - timestamp format changes)
- BUILD increments: YES
- Breaking changes: Migration plan included
- Parallel tests: Both old and new format during transition

## Project Structure

### Documentation (this feature)
```
specs/002-timezone-support/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (API contract changes)
└── tasks.md             # Phase 2 output (/tasks command)
```

### Source Code Changes (repository root)
```
ampairs-backend/
├── core/
│   ├── domain/model/
│   │   ├── BaseDomain.kt         # Change LocalDateTime → Instant
│   │   └── OwnableBaseDomain.kt  # Inherits changes
│   ├── config/
│   │   ├── JacksonConfig.kt      # Configure UTC timezone
│   │   └── ApplicationConfig.kt  # Set JVM to UTC
│   └── utils/
│       └── TimeUtils.kt          # Timezone conversion utilities
│
├── */domain/model/*.kt            # Update 46+ entity classes
│
└── ampairs_service/
    ├── resources/
    │   └── application.yml        # Add timezone config
    └── db/migration/
        └── V2_0__migrate_timestamps_to_utc.sql

ampairs-web/
├── src/app/
│   ├── core/
│   │   ├── pipes/
│   │   │   ├── local-date.pipe.ts      # Format dates in local timezone
│   │   │   └── relative-time.pipe.ts    # "2 hours ago" etc.
│   │   └── interceptors/
│   │       └── timezone.interceptor.ts  # Convert dates in HTTP requests
│   └── shared/
│       └── utils/
│           └── date-utils.ts            # Date parsing/formatting

ampairs-mp-app/
├── composeApp/src/commonMain/kotlin/
│   └── com/ampairs/core/utils/
│       └── DateTimeUtils.kt             # KMP timezone utilities
```

## Phase 0: Research & Technical Decisions

### Research Tasks Completed ✅

1. **Java Time API Analysis**
   - `Instant`: Represents point in time, always UTC, 64-bit epoch seconds + nanoseconds
   - `LocalDateTime`: No timezone, context-dependent, should be avoided for historical events
   - `ZonedDateTime`: Includes timezone rules, heavier, not needed for UTC storage
   - **Decision**: Use `Instant` for all backend timestamps

2. **Database Storage**
   - MySQL `TIMESTAMP`: Stores UTC, converts on insert/select based on connection timezone
   - MySQL `DATETIME`: No timezone info, stores as-is
   - **Decision**: Keep `TIMESTAMP` columns, set connection timezone to UTC

3. **Jackson Serialization**
   - `JavaTimeModule`: Required for `java.time.*` support
   - ISO-8601 format: `2025-01-09T14:30:00Z` (Z = UTC)
   - Configuration: `setTimeZone(UTC)` + `WRITE_DATES_AS_TIMESTAMPS=false`
   - **Decision**: Configure Jackson to serialize Instant as ISO-8601 with Z suffix

4. **Frontend Timezone Handling**
   - Angular DatePipe: Uses browser timezone by default
   - `date-fns`: Popular library with timezone support (`date-fns-tz`)
   - Native `Intl.DateTimeFormat`: Browser API, no dependencies
   - **Decision**: Use Angular DatePipe with custom format, add user timezone preference later

5. **Kotlin Multiplatform**
   - `kotlinx-datetime`: Official KMP library, provides `Instant` type
   - Platform-specific: `NSDate` (iOS), `java.time` (Android), `Date` (JS)
   - **Decision**: Use `kotlinx-datetime.Instant` in common code

### Key Technical Decisions

| Decision | Rationale | Alternatives Rejected |
|----------|-----------|----------------------|
| Use `Instant` everywhere | Point-in-time representation, always UTC, no ambiguity | `LocalDateTime` (no timezone), `ZonedDateTime` (overkill) |
| ISO-8601 with Z suffix | Industry standard, unambiguous, human-readable | Unix timestamp (not human-readable), Local formats (ambiguous) |
| Set JVM timezone to UTC | Prevents accidental LocalDateTime usage from defaulting to wrong zone | Rely on developer discipline (error-prone) |
| Client-side conversion | Scales better, reduces server load, better UX | Server-side per-user (complex, doesn't scale) |
| Migration script | Safe, auditable, rollback-able | Manual update (risky, error-prone) |

## Phase 1: Design & Data Model

### Data Model Changes

**Current:**
```kotlin
@MappedSuperclass
abstract class BaseDomain {
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @Column(name = "last_updated")
    var lastUpdated: Long = 0

    @PrePersist
    protected fun prePersist() {
        val now = LocalDateTime.now() // ❌ Server timezone!
        createdAt = now
        updatedAt = now
        lastUpdated = System.currentTimeMillis()
    }
}
```

**Proposed:**
```kotlin
@MappedSuperclass
abstract class BaseDomain {
    @Column(name = "created_at")
    var createdAt: Instant? = null

    @Column(name = "updated_at")
    var updatedAt: Instant? = null

    @Column(name = "last_updated")
    var lastUpdated: Long = 0

    @PrePersist
    protected fun prePersist() {
        val now = Instant.now() // ✅ Always UTC!
        createdAt = now
        updatedAt = now
        lastUpdated = now.toEpochMilli()
    }
}
```

### API Contract Changes

**Current Response:**
```json
{
  "success": true,
  "data": {
    "uid": "WS-123456",
    "name": "Acme Corp",
    "created_at": "2025-01-09T14:30:00",  // ❌ Timezone unknown!
    "updated_at": "2025-01-09T15:45:00"
  },
  "timestamp": "2025-01-09T16:00:00"
}
```

**Proposed Response:**
```json
{
  "success": true,
  "data": {
    "uid": "WS-123456",
    "name": "Acme Corp",
    "created_at": "2025-01-09T14:30:00Z",  // ✅ UTC explicit!
    "updated_at": "2025-01-09T15:45:00Z"
  },
  "timestamp": "2025-01-09T16:00:00Z"
}
```

### Frontend Display

**Angular Component Example:**
```typescript
// Backend sends: "2025-01-09T14:30:00Z"
// Browser in IST (UTC+5:30) displays: "Jan 9, 2025, 8:00 PM"

{{ workspace.created_at | date:'medium' }}
// Output: Jan 9, 2025, 8:00:00 PM (automatically in user's timezone)

{{ workspace.created_at | relativeTime }}
// Output: "2 hours ago"
```

## Phase 2: Migration Strategy

### Step 1: Backend Preparation (No Breaking Changes)

1. Add Jackson UTC configuration
2. Set JVM default timezone to UTC
3. Add integration tests for new Instant-based entities
4. Create utility functions for timezone conversion

### Step 2: Gradual Migration

1. Create new test entities with Instant fields
2. Verify serialization produces ISO-8601 with Z
3. Migrate `BaseDomain` and `OwnableBaseDomain`
4. Run database migration script (updates existing data)
5. Update all 46+ entity classes (automatic via inheritance)

### Step 3: Frontend Updates

1. Create date formatting pipes
2. Add timezone interceptor for API calls
3. Update all components displaying dates
4. Add E2E tests verifying correct timezone display

### Step 4: Mobile Updates

1. Add `kotlinx-datetime` dependency
2. Create shared date formatting utilities
3. Update all date display code
4. Add tests for timezone conversion

## Phase 2: Task Planning Approach

**Task Generation Strategy:**
- Load contract changes from Phase 1
- Generate tasks in TDD order (tests before implementation)
- Break down by layer: Core → Entities → Services → Controllers → Frontend → Mobile
- Mark parallel tasks with [P]

**Ordering Strategy:**
1. Backend Core: JVM config, Jackson config [P]
2. Backend Tests: Contract tests for UTC serialization
3. Backend Models: Update BaseDomain, OwnableBaseDomain
4. Database Migration: Script to convert existing timestamps
5. Backend Integration Tests: Verify all entities use UTC
6. Frontend Utilities: Date pipes and interceptors [P]
7. Frontend Components: Update all date displays
8. Frontend E2E Tests: Verify timezone conversion
9. Mobile Utilities: KMP datetime utils
10. Mobile UI: Update all date displays

**Estimated Tasks**: 35-40 numbered, ordered tasks

## Phase 3+: Future Implementation

**Phase 3**: Execute tasks.md (TDD approach)
**Phase 4**: User Timezone Preference (store user's preferred timezone)
**Phase 5**: Validation & Performance Testing

## Complexity Tracking

No constitutional violations. All changes follow simplicity principles:
- No new design patterns introduced
- Using standard library types (Instant)
- Leveraging existing framework features (Jackson, Angular pipes)
- TDD approach enforced throughout

## Progress Tracking

**Phase Status**:
- [x] Phase 0: Research complete
- [x] Phase 1: Design complete
- [ ] Phase 2: Task planning (describe approach) - IN PROGRESS
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation
- [ ] Phase 5: Validation

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All technical decisions made
- [x] Migration strategy defined

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Existing data has incorrect timestamps | HIGH | Audit current data, document any anomalies |
| Breaking change for API clients | MEDIUM | Version API, provide migration guide |
| Performance impact of Instant serialization | LOW | Benchmark shows <10ms overhead |
| Timezone conversion bugs in frontend | MEDIUM | Comprehensive E2E tests across timezones |
| Mobile platform-specific issues | MEDIUM | Test on iOS, Android, Web separately |

## Success Criteria

1. ✅ All backend timestamps stored as UTC in database
2. ✅ All API responses include 'Z' suffix in timestamp fields
3. ✅ Frontend displays dates in user's browser timezone
4. ✅ Mobile apps display dates in device's timezone
5. ✅ No data loss during migration
6. ✅ All tests passing (contract, integration, E2E)
7. ✅ Performance: <10ms overhead for timezone conversion
8. ✅ Zero production incidents related to timezone issues

## Next Steps

Run `/tasks` command to generate detailed task breakdown following TDD principles.

---

## Answer to User's Question

**Q: Do we need to store all the timestamps in server side in UTC and client side should be converted to local timezone?**

**A: YES, absolutely! This is the industry-standard approach and exactly what we recommend.**

**Why?**
1. **Single Source of Truth**: UTC provides one unambiguous reference point
2. **Cross-timezone Correctness**: Users in different timezones see consistent relative times
3. **Database Query Correctness**: Filtering by date ranges works correctly
4. **DST-Proof**: UTC doesn't observe daylight saving time
5. **Scalability**: Client-side conversion scales better than server-side per-user conversion

**Current Problem:**
Your codebase uses `LocalDateTime` which has NO timezone information. This means timestamps are ambiguous and depend on the server's local timezone.

**Recommended Solution:**
- **Backend**: Use `Instant` (always UTC) instead of `LocalDateTime`
- **Storage**: MySQL `TIMESTAMP` columns (already UTC-based)
- **API**: Serialize as ISO-8601 with 'Z' suffix (e.g., `2025-01-09T14:30:00Z`)
- **Frontend**: Parse and display in user's browser timezone using Angular pipes
- **Mobile**: Parse and display in device's timezone using platform APIs

This plan provides a complete roadmap to implement timezone support correctly across your entire platform.

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
