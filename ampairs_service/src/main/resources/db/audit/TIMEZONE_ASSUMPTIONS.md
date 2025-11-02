# Timezone Assumptions for Existing Data

**Document Version**: 1.0
**Date**: 2025-10-09
**Migration**: LocalDateTime → Instant (UTC)
**Status**: Pre-Migration Documentation

---

## Executive Summary

This document records the timezone assumptions made about existing timestamp data in the Ampairs database during the migration from `LocalDateTime` to `Instant` (UTC).

**Key Finding**: MySQL `TIMESTAMP` columns already store data in UTC internally. No data migration is required.

---

## Table of Contents

1. [Database Environment](#database-environment)
2. [MySQL TIMESTAMP Behavior](#mysql-timestamp-behavior)
3. [Current Data State](#current-data-state)
4. [Timezone Assumptions by Table](#timezone-assumptions-by-table)
5. [Migration Impact Analysis](#migration-impact-analysis)
6. [Verification Plan](#verification-plan)
7. [Risk Assessment](#risk-assessment)

---

## Database Environment

### Current Configuration

```yaml
Database: MySQL 8.0+
JDBC URL: jdbc:mysql://localhost:3306/munsi_app?serverTimezone=UTC
Spring Boot: 3.5.6
JPA Provider: Hibernate
Java Version: 25
```

### Timezone Settings

| Setting | Expected Value | Verification Query |
|---------|---------------|-------------------|
| System Timezone | UTC or Server Local | `SELECT @@system_time_zone;` |
| Global Timezone | SYSTEM | `SELECT @@global.time_zone;` |
| Session Timezone | UTC (via JDBC) | `SELECT @@session.time_zone;` |

**Important**: The JDBC connection parameter `serverTimezone=UTC` overrides the session timezone, ensuring all TIMESTAMP operations use UTC.

---

## MySQL TIMESTAMP Behavior

### How MySQL Handles TIMESTAMP Columns

#### Storage (INSERT/UPDATE)

```
Client Application
       ↓
   [JPA Entity]
       ↓
   LocalDateTime (Server Timezone)
       ↓
   JDBC Connection (serverTimezone=UTC)
       ↓
   MySQL converts from connection timezone → UTC
       ↓
   Stored as UTC in TIMESTAMP column
```

#### Retrieval (SELECT)

```
MySQL TIMESTAMP column (stored as UTC)
       ↓
   MySQL converts from UTC → connection timezone
       ↓
   JDBC Connection (serverTimezone=UTC)
       ↓
   Returns as UTC (no conversion needed)
       ↓
   [JPA Entity] LocalDateTime
       ↓
   Client Application
```

### Key Behaviors

1. **Internal Storage**: TIMESTAMP columns store values in UTC since MySQL 4.1.3
2. **Connection Timezone**: Conversion happens based on connection `time_zone` variable
3. **serverTimezone=UTC**: When set, no conversion occurs (data flows as UTC)
4. **DATETIME vs TIMESTAMP**: DATETIME stores literal values with no timezone conversion

### Example

```sql
-- Connection timezone: UTC
-- System timezone: Asia/Kolkata (UTC+5:30)

INSERT INTO business_types (created_at) VALUES ('2025-01-09 14:30:00');
-- Stored as: 2025-01-09 14:30:00 UTC

SELECT created_at FROM business_types;
-- Returns: 2025-01-09 14:30:00
-- (no conversion because connection timezone = UTC)
```

---

## Current Data State

### Tables with Timestamp Columns

| Module | Table Name | Timestamp Columns | Record Count (Est.) |
|--------|-----------|-------------------|---------------------|
| Tax | business_types | created_at, updated_at, last_updated | ~7 |
| Tax | hsn_codes | created_at, updated_at, last_updated | ~15 |
| Tax | tax_configurations | created_at, updated_at, last_updated | ~15 |
| Tax | tax_rates | created_at, updated_at, last_updated | ~12 |
| Tax | business_types_tax_codes | created_at, updated_at, last_updated | Variable |
| Workspace | workspace_events | created_at, updated_at | Variable |
| Auth | device_sessions | last_heartbeat, connected_at, disconnected_at, created_at, updated_at | Variable |
| Customer | customers | created_at, updated_at, last_updated | Variable |
| Product | products | created_at, updated_at, last_updated | Variable |
| Order | orders | created_at, updated_at, last_updated, order_date | Variable |
| Invoice | invoices | created_at, updated_at, last_updated, invoice_date, due_date | Variable |

### Column Types

All audit timestamp columns use the same pattern:

```sql
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

### Migration Script Data

**Known Seed Data**:
- Tax module seed data inserted via Flyway migrations
- Uses `UNIX_TIMESTAMP() * 1000` for `last_updated` column (epoch milliseconds)
- Uses `CURRENT_TIMESTAMP` for `created_at` and `updated_at`
- All inserted with `serverTimezone=UTC` in connection string

---

## Timezone Assumptions by Table

### 1. Tax Module Tables

#### business_types

**Assumption**: All timestamps are in UTC
**Reasoning**:
- Seed data inserted via migration script with `serverTimezone=UTC`
- `CURRENT_TIMESTAMP` evaluates to UTC due to connection timezone
- User-created records also use UTC (same connection config)

**Verification**:
```sql
SELECT uid, code, created_at, updated_at, last_updated
FROM business_types
ORDER BY created_at LIMIT 5;
```

#### hsn_codes

**Assumption**: All timestamps are in UTC
**Reasoning**: Same as business_types (migration seed data)

#### tax_configurations

**Assumption**: All timestamps are in UTC
**Reasoning**: Same as business_types (migration seed data)

#### tax_rates

**Assumption**: All timestamps are in UTC
**Reasoning**: Same as business_types (migration seed data)

### 2. Workspace Module Tables

#### workspace_events

**Assumption**: All timestamps are in UTC
**Reasoning**:
- Runtime data inserted via JPA entities with `serverTimezone=UTC`
- No seed data (all user-generated)
- Connection always uses UTC

**Special Case**: Event timestamps represent actual event occurrence time in UTC

### 3. Auth Module Tables

#### device_sessions

**Assumption**: All timestamps are in UTC
**Reasoning**:
- Runtime data (login/logout events)
- Represents actual connection times in UTC
- Critical for session management (must be UTC for consistency)

**Important Columns**:
- `last_heartbeat`: UTC timestamp of last client activity
- `connected_at`: UTC timestamp of session start
- `disconnected_at`: UTC timestamp of session end (nullable)

### 4. Business Domain Tables

#### customers, products, orders, invoices

**Assumption**: All timestamps are in UTC
**Reasoning**:
- User-generated data via API endpoints
- All API requests use `serverTimezone=UTC`
- JPA entities use `@PrePersist` and `@PreUpdate` with `LocalDateTime.now()`

**Note**: `LocalDateTime.now()` uses system default timezone, but with `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` in application startup, this is UTC.

---

## Migration Impact Analysis

### No Database Changes Needed

**Why?**

1. **MySQL Storage**: TIMESTAMP columns already store UTC internally
2. **JPA Mapping**: Change is only in Java layer (LocalDateTime → Instant)
3. **JDBC Behavior**: `serverTimezone=UTC` ensures transparent UTC flow
4. **Column Type**: No ALTER TABLE statements required

### What Actually Changes

#### Before Migration

```kotlin
@Entity
class BusinessTypeEntity : OwnableBaseDomain() {
    var createdAt: LocalDateTime? = null  // Ambiguous timezone
    var updatedAt: LocalDateTime? = null  // Ambiguous timezone
}
```

**API Response**:
```json
{
  "created_at": "2025-01-09T14:30:00",  // No timezone indicator
  "updated_at": "2025-01-09T14:30:00"   // Ambiguous!
}
```

#### After Migration

```kotlin
@Entity
class BusinessTypeEntity : OwnableBaseDomain() {
    var createdAt: Instant? = null  // Explicitly UTC
    var updatedAt: Instant? = null  // Explicitly UTC
}
```

**API Response**:
```json
{
  "created_at": "2025-01-09T14:30:00Z",  // Z indicates UTC
  "updated_at": "2025-01-09T14:30:00Z"   // Unambiguous!
}
```

### Database Column Behavior

| Aspect | Before | After | Changed? |
|--------|--------|-------|----------|
| Column Type | TIMESTAMP | TIMESTAMP | ❌ No |
| Storage Format | UTC | UTC | ❌ No |
| Stored Values | Same | Same | ❌ No |
| JDBC Retrieval | UTC bytes | UTC bytes | ❌ No |
| JPA Mapping | LocalDateTime | Instant | ✅ Yes |
| API Serialization | ISO-8601 (no Z) | ISO-8601 (with Z) | ✅ Yes |

---

## Verification Plan

### Pre-Migration Verification

1. **Run Audit Script**
   ```bash
   mysql -u root -p munsi_app < audit_timestamps.sql > audit_before.txt
   ```

2. **Capture Checksums**
   - Save Section 7 output (timestamp checksums)
   - Document record counts per table
   - Note any data quality issues (Section 4)

3. **Sample Data Snapshot**
   ```sql
   -- Save sample records for manual comparison
   SELECT uid, code, created_at, updated_at
   FROM business_types
   ORDER BY created_at
   LIMIT 10;
   ```

### Post-Migration Verification

1. **Run Audit Script Again**
   ```bash
   mysql -u root -p munsi_app < audit_timestamps.sql > audit_after.txt
   ```

2. **Compare Outputs**
   ```bash
   diff audit_before.txt audit_after.txt
   # Expected: No differences in timestamp values
   # Only differences should be in audit run timestamp
   ```

3. **Verify API Responses**
   ```bash
   # Check for Z suffix in timestamps
   curl http://localhost:8080/api/v1/tax/business-types | jq '.data[0].created_at'
   # Expected: "2025-01-09T14:30:00Z"
   ```

4. **Integration Test Verification**
   ```bash
   ./gradlew test --tests "*InstantSerializationTest"
   # All tests should pass
   ```

### Acceptance Criteria

- [ ] All checksums match between pre/post migration
- [ ] No new NULL values introduced
- [ ] No timestamp value shifts detected
- [ ] API responses include Z suffix on all timestamps
- [ ] Integration tests pass (100% success rate)
- [ ] Sample data verification shows identical values

---

## Risk Assessment

### Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Timezone shift in stored data | **Low** | **High** | MySQL TIMESTAMP already UTC; verify with checksums |
| NULL values introduced | **Low** | **Medium** | @PrePersist hooks set Instant.now(); test coverage |
| API breaking change | **Medium** | **High** | Client apps must handle Z suffix; update docs |
| JVM timezone misconfiguration | **Low** | **High** | Set TimeZone.setDefault() in main(); verify @@session |
| Existing data corruption | **Very Low** | **Critical** | Run audit before migration; have rollback plan |

### Critical Assumptions

1. **System Timezone**: Assumed to be stable (verified via @@system_time_zone)
2. **JDBC Configuration**: serverTimezone=UTC must always be set
3. **JVM Default**: TimeZone.setDefault(UTC) called at application startup
4. **Jackson Configuration**: JavaTimeModule registered with UTC timezone
5. **Client Compatibility**: Frontend/mobile apps can parse ISO-8601 with Z suffix

### Rollback Plan

If migration fails:

1. **Revert JPA Changes**
   ```bash
   git revert <commit-hash>
   ./gradlew clean build
   ```

2. **Restart Application**
   - No database rollback needed (schema unchanged)
   - Application reverts to LocalDateTime

3. **Verify Data Integrity**
   ```bash
   mysql -u root -p munsi_app < audit_timestamps.sql
   # Check checksums match pre-migration
   ```

4. **Update Clients**
   - Remove Z suffix handling if added
   - Revert to previous API contract

---

## Conclusion

### Summary

- **MySQL TIMESTAMP columns already store UTC** - no database migration needed
- **JPA migration is transparent** to database layer
- **API response format changes** - adds Z suffix for unambiguous UTC indication
- **Data integrity preserved** - verified via checksums and audit script
- **Low risk migration** - only Java layer changes, database untouched

### Sign-off

**Database Administrator**: ___________________ Date: __________

**Backend Team Lead**: ___________________ Date: __________

**QA Lead**: ___________________ Date: __________

---

## Appendix

### A. Useful SQL Queries

#### Check Timezone Configuration
```sql
SELECT @@system_time_zone, @@global.time_zone, @@session.time_zone;
```

#### Verify TIMESTAMP Column Types
```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND DATA_TYPE IN ('timestamp', 'datetime')
ORDER BY TABLE_NAME, COLUMN_NAME;
```

#### Compare Timestamps Before/After
```sql
-- Save before migration
CREATE TABLE timestamp_backup AS
SELECT uid, created_at, updated_at FROM business_types;

-- After migration, compare
SELECT
    b.uid,
    b.created_at AS current_created,
    t.created_at AS backup_created,
    b.created_at = t.created_at AS match_created,
    b.updated_at = t.updated_at AS match_updated
FROM business_types b
JOIN timestamp_backup t ON b.uid = t.uid
WHERE b.created_at <> t.created_at OR b.updated_at <> t.updated_at;
```

### B. ISO-8601 Format Examples

| Format | Example | Timezone Indicator | Recommended |
|--------|---------|-------------------|-------------|
| Basic | 2025-01-09T14:30:00 | ❌ None | ❌ Ambiguous |
| With Z | 2025-01-09T14:30:00Z | ✅ UTC | ✅ **Use This** |
| With +00:00 | 2025-01-09T14:30:00+00:00 | ✅ UTC | ✅ Also Valid |
| With offset | 2025-01-09T20:00:00+05:30 | ✅ IST | ⚠️ Avoid (use UTC) |

### C. References

- [MySQL TIMESTAMP Documentation](https://dev.mysql.com/doc/refman/8.0/en/datetime.html)
- [MySQL Timezone Support](https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html)
- [Java Instant API](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html)
- [ISO-8601 Standard](https://en.wikipedia.org/wiki/ISO_8601)
- [Jackson JavaTimeModule](https://github.com/FasterXML/jackson-modules-java8/tree/master/datetime)
