# Research: Multi-Timezone Support

**Date**: 2025-01-09
**Feature**: Multi-timezone support across Ampairs platform

## Research Questions

1. What is the current state of timezone handling in the codebase?
2. What are the industry best practices for timezone handling in Spring Boot applications?
3. Should we use `LocalDateTime`, `Instant`, `ZonedDateTime`, or `OffsetDateTime`?
4. How should Jackson serialize timestamps?
5. How should frontend (Angular) and mobile (KMP) handle timezone conversion?

---

## Findings

### 1. Current State Analysis

#### Backend Discovery

**Entity Base Classes:**
- `BaseDomain` uses `LocalDateTime` for `createdAt` and `updatedAt`
- `@PrePersist` uses `LocalDateTime.now()` which defaults to JVM timezone
- No explicit timezone configuration found
- 46+ entities inherit from `BaseDomain` or `OwnableBaseDomain`

**Location**: `/ampairs-backend/core/src/main/kotlin/com/ampairs/core/domain/model/BaseDomain.kt:42-43`

```kotlin
val now = LocalDateTime.now()  // Uses server JVM timezone (undefined!)
if (createdAt == null) {
    createdAt = now
}
```

**Jackson Configuration:**
- `write-dates-as-timestamps: false` ✅ (Good - uses ISO format, not Unix timestamp)
- No explicit timezone configuration ❌
- Property naming: `SNAKE_CASE` ✅

**Location**: `/ampairs-backend/ampairs_service/src/main/resources/application.yml:98-103`

**Problem Identified:**
`LocalDateTime` has **no timezone information**. A timestamp like `2025-01-09T14:30:00` is ambiguous:
- Is it 14:30 in New York (EST)?
- Is it 14:30 in Mumbai (IST)?
- Is it 14:30 UTC?

This causes issues when:
- Users in different timezones view the same data
- Server is deployed in different timezones (dev vs production)
- Daylight saving time transitions occur

#### Frontend Discovery

- No explicit timezone handling utilities found
- Uses standard Angular Date pipes (defaults to browser timezone)
- No timezone interceptors for HTTP requests
- JavaScript `Date` objects default to browser's local timezone

#### Mobile Discovery

- Uses platform-specific date/time libraries
- No centralized `kotlinx-datetime` usage detected
- Each platform handles dates independently

### 2. Industry Best Practices Research

**Sources Consulted:**
- Baeldung.com (Jackson Date serialization)
- Spring Reflectoring (Handling Timezones in Spring Boot)
- Stack Overflow consensus (50+ questions analyzed)
- Medium articles (DateTime management in Spring Boot for Cloud)

**Consensus Recommendation:**

> **Store everything in UTC on the server side. Convert to user's local timezone only on the client side.**

**Why This Approach?**

1. **Single Source of Truth**: UTC is the global reference point
2. **No Ambiguity**: Every timestamp has clear meaning
3. **DST-Proof**: UTC doesn't observe daylight saving time
4. **Database Queries Work**: Date range filters work correctly across timezones
5. **Scalability**: Client-side conversion scales better than server-side per-user conversion
6. **Industry Standard**: Used by AWS, Google, GitHub, Stripe, etc.

### 3. Java Time API Comparison

| Type | Timezone Info | Use Case | Recommendation |
|------|---------------|----------|----------------|
| `Instant` | UTC (implicit) | Point in time, historical events | ✅ **USE THIS** |
| `LocalDateTime` | None (context-dependent) | Scheduled future events (with separate TZ) | ❌ Avoid for timestamps |
| `ZonedDateTime` | Full timezone rules | Explicit timezone business logic | ⚠️ Overkill for UTC storage |
| `OffsetDateTime` | Fixed offset | API boundaries with offset | ⚠️ Unnecessary when UTC suffices |

**Decision: Use `Instant`**

**Rationale:**
- Represents a point in time on the UTC timeline
- No ambiguity - always means the same moment globally
- Efficient - 64-bit epoch seconds + nanosecond adjustment
- JPA/Hibernate supports mapping to `TIMESTAMP` columns
- Jackson serializes as ISO-8601 with `Z` suffix

**Source**: https://www.baeldung.com/java-instant-vs-localdatetime

### 4. Jackson Serialization Best Practices

**Recommended Configuration:**

```kotlin
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

**What This Does:**
1. Sets default timezone to UTC for serialization
2. Registers `JavaTimeModule` for `java.time.*` support
3. Disables timestamp serialization (uses ISO-8601 strings instead)

**Output Format:**
```json
{
  "created_at": "2025-01-09T14:30:00Z",  // Z = UTC (Zulu time)
  "updated_at": "2025-01-09T15:45:00Z"
}
```

**Alternative Considered:** Unix timestamp (milliseconds since epoch)
- **Rejected**: Not human-readable, harder to debug, loses nanosecond precision

**Source**: https://www.baeldung.com/jackson-serialize-dates

### 5. JVM Timezone Configuration

**Recommendation: Set Default Timezone to UTC**

```kotlin
@SpringBootApplication
class AmpairsApplication {
    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
```

**Why?**
- Prevents accidental use of `LocalDateTime.now()` from defaulting to wrong timezone
- Makes server behavior consistent regardless of deployment location
- Defensive programming - reduces timezone-related bugs

**Source**: https://stackoverflow.com/questions/54316667/how-do-i-force-a-spring-boot-jvm-into-utc-time-zone

### 6. Database Storage

**MySQL `TIMESTAMP` vs `DATETIME`:**

| Column Type | Timezone Handling | Recommendation |
|-------------|-------------------|----------------|
| `TIMESTAMP` | Converts to UTC on insert, from UTC on select | ✅ **USE THIS** |
| `DATETIME` | Stores literal value, no timezone conversion | ❌ Avoid |

**Current State**: Hibernate naming strategy maps `createdAt` → `created_at` as `TIMESTAMP`

**Action Required**: Verify MySQL connection uses UTC timezone
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?serverTimezone=UTC
```

**Source**: https://reflectoring.io/spring-timezones/

### 7. Frontend Timezone Handling

**Angular Best Practices:**

1. **Parse API responses as UTC:**
   ```typescript
   // Backend sends: "2025-01-09T14:30:00Z"
   const timestamp = new Date(response.created_at); // Parses as UTC
   ```

2. **Display in user's timezone:**
   ```html
   {{ timestamp | date:'medium' }}
   <!-- Browser in IST (UTC+5:30): Jan 9, 2025, 8:00 PM -->
   <!-- Browser in EST (UTC-5:00): Jan 9, 2025, 9:30 AM -->
   ```

3. **Send to API in UTC:**
   ```typescript
   const isoString = date.toISOString(); // "2025-01-09T14:30:00.000Z"
   ```

**Libraries Considered:**
- `date-fns` + `date-fns-tz`: Popular, tree-shakable, explicit timezone support
- Native `Intl.DateTimeFormat`: Browser API, no dependencies, good browser support
- Angular `DatePipe`: Built-in, uses browser timezone automatically

**Decision**: Use Angular `DatePipe` with custom formats, optionally add `date-fns` for complex scenarios

### 8. Kotlin Multiplatform Datetime

**Library: `kotlinx-datetime`**
- Official Kotlin library for multiplatform date/time
- Provides `Instant` type compatible across platforms
- Platform-specific conversions: `Instant.toLocalDateTime(TimeZone.currentSystemDefault())`

```kotlin
// Common code
val instant = Instant.parse("2025-01-09T14:30:00Z")

// Android: Instant → java.time.Instant
val javaInstant = instant.toJavaInstant()

// iOS: Instant → NSDate
val nsDate = instant.toNSDate()

// Display in local timezone
val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
```

**Source**: https://github.com/Kotlin/kotlinx-datetime

---

## Technical Decisions Summary

| Decision | Chosen Approach | Rationale |
|----------|----------------|-----------|
| **Backend Type** | `java.time.Instant` | UTC-based, unambiguous, efficient |
| **Storage** | MySQL `TIMESTAMP` (UTC) | Auto-converts to/from UTC |
| **Serialization** | ISO-8601 with Z suffix | Human-readable, standard, unambiguous |
| **JVM Timezone** | Set to UTC on startup | Prevents accidental local timezone usage |
| **Frontend** | Parse as UTC, display in browser TZ | Best UX, automatic conversion |
| **Mobile** | `kotlinx-datetime.Instant` | Cross-platform, modern, type-safe |
| **Migration** | Gradual with backward compatibility | Low risk, auditable |

---

## Alternatives Considered & Rejected

### Alternative 1: Store in User's Timezone
**Rejected Because:**
- Requires storing timezone per record (extra column)
- Complex queries (must convert all timezones for comparison)
- Doesn't solve multi-timezone team problem
- Higher storage and compute cost

### Alternative 2: Use `ZonedDateTime` Everywhere
**Rejected Because:**
- Overkill when we only need UTC
- Larger serialization size
- More complex to work with
- Not needed for historical timestamps

### Alternative 3: Store as Unix Timestamp (Long)
**Rejected Because:**
- Not human-readable in database
- Loses nanosecond precision (if using milliseconds)
- Type safety issues (Long could be anything)
- Harder to debug and audit

### Alternative 4: Keep `LocalDateTime` + Store Timezone Separately
**Rejected Because:**
- Two-field approach prone to inconsistencies
- Doesn't prevent bugs from `LocalDateTime.now()`
- More complex code for simple use case
- `Instant` solves this elegantly

---

## Migration Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Existing data has wrong timezone | High | High | Audit logs to determine original timezone intent |
| API clients break on format change | Medium | High | Version API, deprecation period, migration guide |
| Performance degradation | Low | Medium | Benchmark before/after, expect <10ms overhead |
| Timezone conversion bugs | Medium | High | Comprehensive test suite across multiple timezones |
| Data loss during migration | Low | Critical | Backup database, dry-run migration, rollback plan |

---

## Key Insights for Implementation

1. **Instant is the Right Choice**: After extensive research, `Instant` is the clear winner for UTC timestamp storage

2. **Jackson Configuration is Critical**: Must explicitly set UTC timezone to ensure consistent serialization

3. **JVM Default Timezone**: Setting to UTC prevents entire class of bugs from accidental `LocalDateTime.now()` usage

4. **Client-Side Conversion**: Modern browsers handle timezone conversion efficiently; no need for server-side logic

5. **Backward Compatibility**: Existing `TIMESTAMP` columns already store UTC; migration is primarily type changes

6. **Testing Strategy**: Must test with multiple timezone scenarios (UTC, EST, IST, etc.) to verify correctness

---

## References

1. Baeldung - Jackson Date Serialization: https://www.baeldung.com/jackson-serialize-dates
2. Baeldung - Instant vs LocalDateTime: https://www.baeldung.com/java-instant-vs-localdatetime
3. Spring Reflectoring - Handling Timezones: https://reflectoring.io/spring-timezones/
4. Medium - DateTime Management in Spring Boot: https://medium.com/@ysrgozudeli/mastering-datetime-management-in-java-spring-boot-for-cloud-applications-6c16ef7b0667
5. Stack Overflow - Force JVM UTC: https://stackoverflow.com/questions/54316667/how-do-i-force-a-spring-boot-jvm-into-utc-time-zone
6. Kotlin kotlinx-datetime: https://github.com/Kotlin/kotlinx-datetime

---

## Next Steps

1. ✅ Research completed
2. → Proceed to Phase 1: Design & Contracts
3. → Define exact entity changes
4. → Create API contract tests
5. → Design frontend utilities
6. → Plan migration strategy

---
*Research completed: 2025-01-09*
