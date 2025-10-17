# Quickstart Guide: Database Schema Migration with Flyway

**Feature**: Database Schema Migration with Flyway
**Date**: 2025-10-12
**For**: Developers implementing Flyway migrations for existing JPA entities

## Prerequisites

- Understanding of JPA/Hibernate entity mappings
- Familiarity with MySQL 8.0 SQL syntax
- Access to existing entity source code
- Docker (for local MySQL testing)

## Quick Overview

This feature creates Flyway migration scripts for 22+ existing JPA entities across 6 modules to enable production deployment with `ddl-auto: validate` configuration.

**Migration Files**: V4_1 through V4_10 (10 files)
**Location**: `{module}/src/main/resources/db/migration/mysql/` (e.g., `ampairs-backend/unit/...`)
**Pattern**: `V{major}_{minor}__{description}.sql`

---

## Step 1: Understand the Entity

Before creating a migration, analyze the JPA entity:

```kotlin
// Example: Unit.kt
@Entity(name = "unit")
@Table(indexes = [
    Index(name = "unit_idx", columnList = "name"),
    Index(name = "idx_unit_uid", columnList = "uid", unique = true)
])
class Unit : OwnableBaseDomain() {
    @Column(name = "name", length = 10, nullable = false)
    var name: String = ""

    @Column(name = "short_name", length = 10, nullable = false)
    var shortName: String = ""

    @Column(name = "decimal_places", nullable = false)
    var decimalPlaces: Int = 2

    @Column(name = "active", nullable = false)
    var active: Boolean = true
}
```

**Key Information Needed**:
- Base class: `BaseDomain` or `OwnableBaseDomain`?
- Column definitions: name, type, length, nullable
- Indexes: `@Table(indexes = [...])`
- Relationships: `@ManyToOne`, `@OneToOne`, etc.
- JSON columns: `@JdbcTypeCode(SqlTypes.JSON)`

---

## Step 2: Create Migration File

Create file in the owning module's migration directory:

```bash
cd ampairs-backend/unit/src/main/resources/db/migration/mysql/
touch V4_2__create_unit_module_tables.sql
```

**Naming Rules**:
- `V` prefix (uppercase)
- Major version: 4 (retail management feature)
- Minor version: Sequential (1, 2, 3...)
- Double underscore `__` separator
- Description: lowercase with underscores

---

## Step 3: Write Migration SQL

Use the template from [contracts/migration-template.sql](./contracts/migration-template.sql):

```sql
-- =====================================================
-- Unit Module Database Migration Script
-- =====================================================
-- Version: 4.2
-- Description: Create unit and unit_conversion tables
-- Author: Your Name
-- Date: 2025-10-12
--
-- Tables Created:
-- 1. unit - Base unit of measurement
-- 2. unit_conversion - Conversion ratios between units
--
-- Dependencies:
-- - Requires: V3_2 (baseline tables)
-- - Required By: V4_3 (product module uses unit)
-- =====================================================

-- =====================================================
-- Unit Table
-- =====================================================
CREATE TABLE unit (
    -- BaseDomain fields
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    -- OwnableBaseDomain fields
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Entity-specific fields
    name VARCHAR(10) NOT NULL,
    short_name VARCHAR(10) NOT NULL,
    decimal_places INT NOT NULL DEFAULT 2,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    INDEX unit_idx (name),
    UNIQUE INDEX idx_unit_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Unit Conversion Table
-- =====================================================
CREATE TABLE unit_conversion (
    -- BaseDomain fields
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    -- OwnableBaseDomain fields
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Entity-specific fields
    base_unit_id VARCHAR(200) NOT NULL,
    derived_unit_id VARCHAR(200) NOT NULL,
    multiplier DOUBLE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_unit_conversion_uid (uid),
    INDEX idx_unit_conversion_base (base_unit_id),
    INDEX idx_unit_conversion_derived (derived_unit_id),
    FOREIGN KEY (base_unit_id) REFERENCES unit(uid) ON DELETE CASCADE,
    FOREIGN KEY (derived_unit_id) REFERENCES unit(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Step 4: Mapping Reference

### Common JPA  SQL Mappings

| JPA Type | SQL Type | Notes |
|----------|----------|-------|
| `String` | `VARCHAR(length)` | Default length: 255 |
| `String` with `columnDefinition = "TEXT"` | `TEXT` | Long text |
| `Int` | `INT` | Integer |
| `Long` | `BIGINT` | Long integer |
| `Double` | `DOUBLE` | Decimal |
| `Boolean` | `BOOLEAN` | MySQL uses TINYINT(1) |
| `Instant` | `TIMESTAMP` | **NOT DATETIME** |
| `Date` | `TIMESTAMP` | Legacy, migrating to Instant |
| `LocalDateTime` | `TIMESTAMP` | Legacy, avoid in new code |
| `@JdbcTypeCode(SqlTypes.JSON)` | `JSON` | MySQL 8.0 native JSON |
| `@Type(JsonType::class)` | `json` | Hibernate JSON type |
| `Point` | `POINT` | Spatial type |

### Relationship Mappings

| JPA Annotation | SQL Action | CASCADE Rule |
|----------------|------------|--------------|
| `@OneToMany` | `FOREIGN KEY ... ON DELETE CASCADE` | Delete children |
| `@ManyToOne` | `FOREIGN KEY ... ON DELETE RESTRICT` | Prevent parent delete |
| `@OneToOne` (nullable) | `FOREIGN KEY ... ON DELETE SET NULL` | Null reference |
| `@OneToOne` (not null) | `FOREIGN KEY ... ON DELETE CASCADE` | Delete cascade |

---

## Step 5: Test Migration Locally

### Start MySQL Container

```bash
docker run -d \
  --name mysql-flyway-test \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=test_db \
  -p 3307:3306 \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### Update Application Properties (Test)

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/test_db?serverTimezone=UTC
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate  # MUST be validate
  flyway:
    enabled: true
```

### Run Application

```bash
cd ampairs-backend
./gradlew :ampairs_service:bootRun --args='--spring.profiles.active=test'
```

### Verify Migration

Check logs for:
```
INFO  - Flyway Community Edition
INFO  - Migrating schema `test_db` to version 4.2 - create unit module tables
INFO  - Successfully applied 1 migration to schema `test_db`
```

### Inspect Database

```bash
docker exec -it mysql-flyway-test mysql -uroot -proot test_db

mysql> SHOW TABLES;
mysql> DESCRIBE unit;
mysql> DESCRIBE unit_conversion;
mysql> SHOW INDEX FROM unit_conversion;
mysql> SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## Step 6: Validate Schema Matches Entities

The application will fail to start if schema doesn't match entities (due to `ddl-auto: validate`).

**Success**:
```
INFO  - HHH000400: Using dialect: org.hibernate.dialect.MySQLDialect
INFO  - Started AmpairsServiceApplication in 5.234 seconds
```

**Failure**:
```
ERROR - Schema-validation: wrong column type encountered in column [created_at]
ERROR - in table [unit]; found [datetime], but expecting [timestamp]
```

If validation fails, fix the migration SQL and increment version (e.g., V4_10__fix_notification_queue.sql).

---

## Step 7: Write Tests

Create Testcontainers test:

```kotlin
@SpringBootTest
@Testcontainers
class FlywayMigrationTest {

    companion object {
        @Container
        val mysql = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("test_db")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @Test
    fun `should execute migration V4_2 successfully`() {
        // If test passes, migration executed and validated
        assertTrue(true)
    }
}
```

Run test:
```bash
./gradlew :ampairs_service:test --tests FlywayMigrationTest
```

---

## Step 8: Deploy to Staging

1. **Backup Staging Database**:
```bash
mysqldump -u user -p munsi_app > backup_staging_$(date +%Y%m%d).sql
```

2. **Deploy Application**:
```bash
export SPRING_PROFILES_ACTIVE=staging
export JPA_DDL_AUTO=validate
export FLYWAY_ENABLED=true
./gradlew :ampairs_service:bootRun
```

3. **Verify Migration**:
```sql
SELECT * FROM flyway_schema_history WHERE version = '4.2';
SHOW TABLES LIKE 'unit%';
```

4. **Test CRUD Operations**:
```bash
curl -X POST http://staging/api/v1/unit -H "Content-Type: application/json" -d '{"name":"KG","shortName":"kg"}'
curl -X GET http://staging/api/v1/unit
```

---

## Common Issues and Solutions

### Issue 1: Foreign Key Constraint Error

**Error**:
```
ERROR 1215: Cannot add foreign key constraint 'fk_unit_conversion_base_unit'
```

**Causes**:
- Parent table doesn't exist
- Column type mismatch (VARCHAR(200) vs VARCHAR(255))
- Referenced column doesn't have unique index

**Solution**:
```sql
-- Verify parent table exists
SHOW TABLES LIKE 'unit';

-- Verify column types match
DESCRIBE unit;  -- Check uid is VARCHAR(200)
DESCRIBE unit_conversion;  -- Check base_unit_id is VARCHAR(200)

-- Verify unique index on referenced column
SHOW INDEX FROM unit WHERE Key_name = 'uid';
```

### Issue 2: JPA Validation Fails

**Error**:
```
Schema-validation: wrong column type encountered in column [created_at]
found [datetime], but expecting [timestamp]
```

**Solution**: Use `TIMESTAMP`, not `DATETIME`:
```sql
-- WRONG
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

-- CORRECT
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
```

### Issue 3: Checksum Mismatch

**Error**:
```
ERROR: Migration checksum mismatch for migration 4.2
```

**Cause**: Migration file modified after execution

**Solution**:
```bash
# Option 1: Repair (updates checksum)
./gradlew flywayRepair

# Option 2: Create new migration (recommended)
touch V4_10__fix_notification_queue.sql
```

---

## Cheat Sheet

### Create New Migration

```bash
# 1. Create file
touch V4_{n}__create_{module}_tables.sql

# 2. Add header
cat <<EOF > V4_{n}__create_{module}_tables.sql
-- =====================================================
-- {Module} Module Database Migration Script
-- =====================================================
-- Version: 4.{n}
-- Description: Create {module} tables
-- Author: $(git config user.name)
-- Date: $(date +%Y-%m-%d)
-- =====================================================
EOF

# 3. Add table DDL (use template)

# 4. Test locally
docker-compose up mysql
./gradlew :ampairs_service:bootRun

# 5. Verify
docker exec mysql mysql -e "SELECT * FROM flyway_schema_history;"

# 6. Run tests
./gradlew :ampairs_service:test
```

### Flyway Commands

```bash
# Status
./gradlew flywayInfo

# Migrate
./gradlew flywayMigrate

# Validate
./gradlew flywayValidate

# Baseline (production only)
./gradlew flywayBaseline -Pflyway.baselineVersion=4.7
```

### Database Commands

```sql
-- Check migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Check tables
SHOW TABLES;

-- Check structure
DESCRIBE table_name;

-- Check foreign keys
SELECT * FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME IS NOT NULL
AND TABLE_SCHEMA = 'munsi_app';
```

---

## Next Steps

1. Read [research.md](./research.md) for detailed JPA-to-DDL patterns
2. Review [data-model.md](./data-model.md) for entity mappings
3. Use [contracts/migration-template.sql](./contracts/migration-template.sql) as starting point
4. Create migration files V4_1 through V4_10
5. Test each migration independently
6. Run full test suite
7. Deploy to staging
8. Monitor production deployment

---

## References

- **Research Document**: [research.md](./research.md) - Comprehensive migration patterns
- **Data Model**: [data-model.md](./data-model.md) - Entity-to-table mappings
- **Implementation Plan**: [plan.md](./plan.md) - Technical context
- **Feature Spec**: [spec.md](./spec.md) - Requirements and success criteria
- **Flyway Documentation**: https://documentation.red-gate.com/fd/
- **MySQL 8.0 Reference**: https://dev.mysql.com/doc/refman/8.0/en/
