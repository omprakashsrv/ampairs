# Database Migration Guide - Flyway Configuration

## Overview

Ampairs uses **Flyway** for all database schema changes (DDL operations). Hibernate's auto-DDL feature is **DISABLED** to ensure controlled, versioned migrations.

## ⚠️ CRITICAL: Hibernate DDL Auto Configuration

### Current Settings

**Development** (`application.yml`):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Only validates, DOES NOT create/update schema
```

**Production** (`application-production.yml`):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Never use 'update' or 'create' in production
```

**Testing** (`application-test.yml`):
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # OK for tests only - creates fresh schema each run
  flyway:
    enabled: false  # Let Hibernate manage schema in tests
```

### DDL-Auto Options Explained

| Option | Behavior | Use Case | Risk Level |
|--------|----------|----------|------------|
| `validate` | Validates entity mapping, NO schema changes | ✅ **Production/Development** | 🟢 Low |
| `none` | No validation or schema generation | Legacy databases | 🟡 Medium |
| `create` | Drops and recreates schema on startup | ❌ NEVER in production | 🔴 CRITICAL |
| `create-drop` | Creates on startup, drops on shutdown | ✅ Tests only | 🔴 CRITICAL |
| `update` | Updates schema automatically | ❌ NEVER use | 🔴 HIGH |

### Why NOT to use `update`

1. **No rollback**: Changes are irreversible
2. **Data loss risk**: Can drop columns with data
3. **No audit trail**: No record of what changed
4. **Concurrent issues**: Multiple instances can conflict
5. **Production risk**: Unexpected schema changes on deployment

## Flyway Configuration

### Current Setup

**Development** (`application.yml`):
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
    locations: classpath:db/migration
    user: ${FLYWAY_USER:${spring.datasource.username}}
    password: ${FLYWAY_PASSWORD:${spring.datasource.password}}
```

**Production** (`application-production.yml`):
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: false  # Set to true only for existing databases
    validate-on-migrate: true
    out-of-order: false
    locations: classpath:db/migration
    user: ${FLYWAY_USER:${spring.datasource.username}}
    password: ${FLYWAY_PASSWORD:${spring.datasource.password}}
    placeholder-replacement: true
    placeholders:
      environment: production
```

### Flyway Settings Explained

| Setting | Development | Production | Purpose |
|---------|-------------|------------|---------|
| `enabled` | `true` | `true` | Enable Flyway migrations |
| `baseline-on-migrate` | `true` | `false` | Create baseline for existing DBs |
| `validate-on-migrate` | `true` | `true` | Validate migrations before applying |
| `out-of-order` | `false` | `false` | Disallow out-of-order migrations |
| `locations` | `classpath:db/migration` | `classpath:db/migration` | Migration scripts location |

## Database User Permissions

### Required Permissions

Flyway requires a database user with **DDL permissions** to create, alter, and drop database objects.

### MySQL Permissions

#### Create User and Grant Permissions
```sql
-- Create Flyway user
CREATE USER 'ampairs_flyway'@'%' IDENTIFIED BY 'secure_password';

-- Grant DDL permissions for migrations
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES ON ampairs_db.* TO 'ampairs_flyway'@'%';

-- Grant DML permissions for data operations
GRANT SELECT, INSERT, UPDATE, DELETE ON ampairs_db.* TO 'ampairs_flyway'@'%';

-- Grant permissions on Flyway schema history table
GRANT CREATE, INSERT, UPDATE, DELETE, SELECT ON ampairs_db.flyway_schema_history TO 'ampairs_flyway'@'%';

-- Apply changes
FLUSH PRIVILEGES;
```

#### Verify Permissions
```sql
SHOW GRANTS FOR 'ampairs_flyway'@'%';
```

#### Minimal Permissions (if needed)
```sql
-- Absolute minimum for Flyway to work
GRANT CREATE, ALTER, DROP, INDEX ON ampairs_db.* TO 'ampairs_flyway'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON ampairs_db.* TO 'ampairs_flyway'@'%';
```

### PostgreSQL Permissions

#### Create User and Grant Permissions
```sql
-- Create Flyway user
CREATE USER ampairs_flyway WITH PASSWORD 'secure_password';

-- Grant database-level privileges
GRANT ALL PRIVILEGES ON DATABASE ampairs_db TO ampairs_flyway;

-- Connect to the database
\c ampairs_db

-- Grant schema-level privileges
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ampairs_flyway;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ampairs_flyway;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO ampairs_flyway;

-- Grant default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO ampairs_flyway;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO ampairs_flyway;
```

#### Verify Permissions
```sql
\du ampairs_flyway
SELECT * FROM information_schema.role_table_grants WHERE grantee = 'ampairs_flyway';
```

#### Minimal Permissions (if needed)
```sql
GRANT CREATE, CONNECT ON DATABASE ampairs_db TO ampairs_flyway;
GRANT CREATE, USAGE ON SCHEMA public TO ampairs_flyway;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ampairs_flyway;
```

## Environment Variable Configuration

### Development (.env or IDE)
```bash
# Database Connection
DB_URL=jdbc:mysql://localhost:3306/ampairs_dev
DB_USERNAME=ampairs_flyway
DB_PASSWORD=secure_password

# Flyway Configuration (optional - defaults to datasource)
FLYWAY_USER=ampairs_flyway
FLYWAY_PASSWORD=secure_password
FLYWAY_ENABLED=true
```

### Production (.env or System Environment)
```bash
# Database Connection
DB_URL=jdbc:postgresql://prod-db.example.com:5432/ampairs_prod
DB_USERNAME=ampairs_flyway
DB_PASSWORD=very_secure_production_password

# Flyway Configuration
FLYWAY_USER=ampairs_flyway
FLYWAY_PASSWORD=very_secure_production_password
FLYWAY_ENABLED=true

# JPA Configuration
JPA_DDL_AUTO=validate  # CRITICAL: Never use 'update'
```

## Migration File Structure

```
ampairs-backend/
├── business/src/main/resources/db/migration/
│   ├── V1.0.0__create_businesses_table.sql
│   └── V1.0.1__migrate_workspace_business_data.sql
├── customer/src/main/resources/db/migration/
│   └── V1.0.0__create_customers_table.sql
├── product/src/main/resources/db/migration/
│   └── V1.0.0__create_products_table.sql
└── [other modules...]
```

## Migration Naming Convention

**Format**: `V{version}__{description}.sql`

**Examples**:
- `V1.0.0__create_businesses_table.sql` - Initial table creation
- `V1.0.1__migrate_workspace_business_data.sql` - Data migration
- `V1.1.0__add_logo_field_to_business.sql` - Add new column
- `V1.2.0__create_business_hours_table.sql` - New related table

**Rules**:
1. Version must be unique and sequential
2. Underscores separate version from description
3. Description uses underscores (not spaces)
4. SQL files only (no Java-based migrations in this project)

## Common Operations

### 1. Check Migration Status
```bash
./gradlew :ampairs_service:flywayInfo
```

### 2. Validate Migrations
```bash
./gradlew :ampairs_service:flywayValidate
```

### 3. Apply Pending Migrations
```bash
# Migrations are applied automatically on application startup
./gradlew :ampairs_service:bootRun

# Or manually trigger
./gradlew :ampairs_service:flywayMigrate
```

### 4. Baseline Existing Database
```bash
# For existing databases that don't have Flyway history
./gradlew :ampairs_service:flywayBaseline
```

### 5. Repair Flyway Metadata
```bash
# If migration checksums fail (use with caution)
./gradlew :ampairs_service:flywayRepair
```

## Troubleshooting

### Issue: "Unsupported Database: MySQL 8.4"

**Solution**: Ensure Flyway MySQL driver is in dependencies:
```kotlin
implementation("org.flywaydb:flyway-mysql")
```

### Issue: "Validation failed: Migration checksum mismatch"

**Cause**: Migration file was modified after being applied

**Solutions**:
1. **Never modify applied migrations** - create a new migration instead
2. If absolutely necessary: `./gradlew flywayRepair` (⚠️ use with caution)

### Issue: "Access denied" during migration

**Cause**: Database user lacks DDL permissions

**Solution**: Grant proper permissions (see MySQL/PostgreSQL sections above)

### Issue: "Table already exists"

**Cause**: Schema was created by Hibernate before Flyway was enabled

**Solution**: 
```bash
# Baseline the existing database
FLYWAY_BASELINE_ON_MIGRATE=true ./gradlew bootRun
```

Or in `application.yml`:
```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

## Best Practices

### ✅ DO

1. **Always use Flyway** for schema changes
2. **Test migrations** in development before production
3. **Version control** all migration scripts
4. **Use validate mode** in production (`ddl-auto: validate`)
5. **Grant DDL permissions** to Flyway user
6. **Never modify** applied migrations
7. **Create new migrations** for changes
8. **Use descriptive names** for migrations
9. **Include rollback** instructions in comments
10. **Test migrations** with production-like data

### ❌ DON'T

1. **Never use `ddl-auto: update`** in any environment (except tests with `create-drop`)
2. **Never modify** applied migration files
3. **Never skip** migration validation
4. **Never use** out-of-order migrations in production
5. **Never grant** excessive permissions to application user
6. **Never run** Flyway manually without understanding the impact
7. **Never delete** migration files from version control
8. **Never reuse** version numbers

## Security Considerations

### Separate Users (Recommended for Production)

**Flyway User** (DDL permissions):
- Used only during deployment/migration
- Has CREATE, ALTER, DROP permissions
- Should be disabled/locked after migration

**Application User** (DML permissions only):
- Used by running application
- Has SELECT, INSERT, UPDATE, DELETE only
- NO DDL permissions (CREATE, ALTER, DROP)

### Implementation

```yaml
# application-production.yml
spring:
  datasource:
    username: ${DB_APP_USER}        # Limited permissions
    password: ${DB_APP_PASSWORD}
  flyway:
    user: ${DB_FLYWAY_USER}         # DDL permissions
    password: ${DB_FLYWAY_PASSWORD}
```

```sql
-- MySQL: Create separate users
CREATE USER 'ampairs_app'@'%' IDENTIFIED BY 'app_password';
CREATE USER 'ampairs_flyway'@'%' IDENTIFIED BY 'flyway_password';

-- App user: DML only
GRANT SELECT, INSERT, UPDATE, DELETE ON ampairs_db.* TO 'ampairs_app'@'%';

-- Flyway user: DDL + DML
GRANT ALL ON ampairs_db.* TO 'ampairs_flyway'@'%';
```

## Module-Specific Migrations

Each module manages its own migrations:

| Module | Migration Location | Purpose |
|--------|-------------------|---------|
| `business` | `business/src/main/resources/db/migration/` | Business profiles |
| `customer` | `customer/src/main/resources/db/migration/` | Customer management |
| `product` | `product/src/main/resources/db/migration/` | Product catalog |
| `order` | `order/src/main/resources/db/migration/` | Order processing |
| `invoice` | `invoice/src/main/resources/db/migration/` | Invoicing |
| `workspace` | `workspace/src/main/resources/db/migration/` | Multi-tenancy |

Flyway automatically discovers all migrations from all modules on the classpath.

## References

- [Flyway Documentation](https://documentation.red-gate.com/fd)
- [Flyway MySQL Guide](https://documentation.red-gate.com/fd/mysql-184127601.html)
- [Flyway PostgreSQL Guide](https://documentation.red-gate.com/fd/postgresql-184127604.html)
- [Spring Boot Flyway Integration](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.flyway)

---

**Last Updated**: October 10, 2025  
**Status**: ✅ PRODUCTION READY
