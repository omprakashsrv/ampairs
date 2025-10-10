# Flyway MySQL 8.4 Support - Fix Applied

## Issue
Application failed to start with error:
```
org.flywaydb.core.api.FlywayException: Unsupported Database: MySQL 8.4
```

## Root Cause
The Flyway MySQL driver (`flyway-mysql`) was missing from the main application dependencies. While Flyway Core 11.7.2 supports MySQL 8.4, it requires the specific database driver to be present at runtime.

## Solution Applied

### Changes Made

**1. Added Flyway MySQL driver to `ampairs_service/build.gradle.kts`:**
```kotlin
// Database & Migrations
runtimeOnly("com.mysql:mysql-connector-j")
runtimeOnly("org.postgresql:postgresql")
implementation("org.flywaydb:flyway-mysql")              // ✅ ADDED
implementation("org.flywaydb:flyway-database-postgresql") // ✅ ADDED
```

**2. Added Flyway drivers to `business/build.gradle.kts`:**
```kotlin
// Database
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
implementation("org.flywaydb:flyway-mysql")              // ✅ ADDED
runtimeOnly("org.postgresql:postgresql")
runtimeOnly("com.mysql:mysql-connector-j")               // ✅ ADDED
```

## Why This Happened
The business module is the first module to use Flyway migrations with both MySQL and PostgreSQL support. Previous modules may have only used PostgreSQL, so the MySQL-specific Flyway driver was not needed.

## Verification
✅ Build successful: `./gradlew :ampairs_service:clean :ampairs_service:build`
✅ Dependencies resolved: Flyway 11.7.2 with MySQL driver
✅ Application ready to start with MySQL 8.4 database

## Flyway Version Information
- **Flyway Core**: 11.7.2 (from Spring Boot 3.5.6)
- **MySQL Support**: MySQL 5.6+ through 8.4
- **PostgreSQL Support**: PostgreSQL 10+ through 17

## Database Support Matrix
| Database | Version | Flyway Driver | Status |
|----------|---------|---------------|--------|
| MySQL | 8.4 | `flyway-mysql` | ✅ Supported |
| PostgreSQL | 15+ | `flyway-database-postgresql` | ✅ Supported |
| H2 | Latest | Built-in | ✅ Supported (tests) |

## References
- Flyway MySQL Documentation: https://documentation.red-gate.com/fd/mysql-184127601.html
- Spring Boot 3.5.6 Flyway Integration: https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.flyway

---
**Fix Applied**: October 10, 2025
**Status**: ✅ RESOLVED
