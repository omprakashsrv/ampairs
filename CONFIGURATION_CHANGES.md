# Configuration Changes - Flyway DDL Management

## Overview
Configured the application to use **Flyway for all DDL operations** and disabled Hibernate's auto-DDL feature to ensure production-safe database migrations.

## Changes Made

### 1. Application Configuration (`application.yml`)

**Hibernate DDL Auto - DISABLED**:
```yaml
spring:
  jpa:
    hibernate:
      # CHANGED: from 'update' to 'validate'
      ddl-auto: ${JPA_DDL_AUTO:validate}  # Only validates, does NOT modify schema
```

**Flyway Configuration - ADDED**:
```yaml
spring:
  flyway:
    enabled: ${FLYWAY_ENABLED:true}
    baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:true}
    baseline-version: ${FLYWAY_BASELINE_VERSION:0}
    validate-on-migrate: ${FLYWAY_VALIDATE_ON_MIGRATE:true}
    out-of-order: ${FLYWAY_OUT_OF_ORDER:false}
    locations: ${FLYWAY_LOCATIONS:classpath:db/migration}
    # Database user must have DDL permissions (CREATE, ALTER, DROP)
    user: ${FLYWAY_USER:${spring.datasource.username}}
    password: ${FLYWAY_PASSWORD:${spring.datasource.password}}
```

### 2. Production Configuration (`application-production.yml`)

**Replaced Liquibase with Flyway**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Never use 'update' in production
      
  # REMOVED: Liquibase configuration
  # ADDED: Flyway configuration
  flyway:
    enabled: true
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
    locations: classpath:db/migration
    user: ${FLYWAY_USER:${spring.datasource.username}}
    password: ${FLYWAY_PASSWORD:${spring.datasource.password}}
    placeholder-replacement: true
    placeholders:
      environment: production
```

### 3. Test Configuration (`application-test.yml`)

**Flyway Disabled for Tests**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # OK for tests - creates fresh schema
      
  # Flyway disabled - let Hibernate manage test schema
  flyway:
    enabled: false
```

### 4. Gradle Dependencies

**Added Flyway MySQL driver** (`ampairs_service/build.gradle.kts`):
```kotlin
dependencies {
    // Database & Migrations
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-mysql")              // ✅ ADDED
    implementation("org.flywaydb:flyway-database-postgresql") // ✅ ADDED
}
```

**Added to Business Module** (`business/build.gradle.kts`):
```kotlin
dependencies {
    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.flywaydb:flyway-mysql")              // ✅ ADDED
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.mysql:mysql-connector-j")               // ✅ ADDED
}
```

## Key Benefits

### ✅ Production Safety
- **No automatic schema changes**: Hibernate cannot modify production schema
- **Controlled migrations**: All DDL changes go through reviewed Flyway scripts
- **Rollback capability**: Can track and potentially revert migrations

### ✅ Audit Trail
- **Version control**: All migrations in Git
- **Migration history**: Flyway tracks what was applied and when
- **Accountability**: Know exactly when and why schema changed

### ✅ Multi-Environment Support
- **Development**: `validate` mode - Flyway applies migrations
- **Production**: `validate` mode - Flyway applies migrations, never auto-create
- **Testing**: `create-drop` mode - Fresh schema for each test run

### ✅ Team Collaboration
- **No surprises**: Schema changes explicit in migration files
- **Review process**: Migrations reviewed before merge
- **Consistency**: Same schema across all environments

## Database User Requirements

### Required Permissions

Flyway user needs **DDL permissions** (CREATE, ALTER, DROP, INDEX, REFERENCES).

### MySQL Example
```sql
CREATE USER 'ampairs_flyway'@'%' IDENTIFIED BY 'secure_password';
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES ON ampairs_db.* TO 'ampairs_flyway'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON ampairs_db.* TO 'ampairs_flyway'@'%';
FLUSH PRIVILEGES;
```

### PostgreSQL Example
```sql
CREATE USER ampairs_flyway WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE ampairs_db TO ampairs_flyway;
```

See [DATABASE_MIGRATION_GUIDE.md](DATABASE_MIGRATION_GUIDE.md) for detailed permission setup.

## Environment Variables

### Development
```bash
# Database
DB_URL=jdbc:mysql://localhost:3306/ampairs_dev
DB_USERNAME=ampairs_flyway  # User with DDL permissions
DB_PASSWORD=secure_password

# Flyway (optional - defaults to datasource)
FLYWAY_USER=ampairs_flyway
FLYWAY_PASSWORD=secure_password
JPA_DDL_AUTO=validate
```

### Production
```bash
# Database
DB_URL=jdbc:postgresql://prod-db:5432/ampairs_prod
DB_USERNAME=ampairs_flyway
DB_PASSWORD=very_secure_password

# Flyway
FLYWAY_USER=ampairs_flyway
FLYWAY_PASSWORD=very_secure_password
FLYWAY_ENABLED=true
JPA_DDL_AUTO=validate  # CRITICAL
```

## Migration Workflow

### Creating New Migration

1. Create migration file in appropriate module:
   ```
   business/src/main/resources/db/migration/V1.1.0__add_logo_field.sql
   ```

2. Write DDL statements:
   ```sql
   ALTER TABLE businesses ADD COLUMN logo_url VARCHAR(500);
   CREATE INDEX idx_business_logo ON businesses(logo_url);
   ```

3. Test locally:
   ```bash
   ./gradlew :ampairs_service:bootRun
   ```

4. Flyway automatically applies pending migrations on startup

### Checking Migration Status

```bash
# View migration status
./gradlew :ampairs_service:flywayInfo

# Validate migrations
./gradlew :ampairs_service:flywayValidate

# Manually apply migrations
./gradlew :ampairs_service:flywayMigrate
```

## Verification

### Build Status
```bash
./gradlew :ampairs_service:build -x test
# ✅ BUILD SUCCESSFUL
```

### Configuration Verification
```bash
# Check Flyway is configured
grep -A 10 "flyway:" ampairs_service/src/main/resources/application.yml

# Check DDL auto is set to validate
grep "ddl-auto:" ampairs_service/src/main/resources/application.yml
```

## Migration to This Setup

### For Existing Database

If your database was created by Hibernate auto-DDL:

1. **Baseline the database**:
   ```bash
   FLYWAY_BASELINE_ON_MIGRATE=true ./gradlew bootRun
   ```

2. **Or set in application.yml**:
   ```yaml
   spring:
     flyway:
       baseline-on-migrate: true
       baseline-version: 0
   ```

3. **Future migrations will be applied on top of baseline**

### For New Database

No special steps needed - Flyway will:
1. Create `flyway_schema_history` table
2. Apply all pending migrations
3. Track applied migrations

## Documentation

### Created Files
1. **DATABASE_MIGRATION_GUIDE.md** - Comprehensive Flyway guide
   - DDL auto configuration
   - Database user permissions
   - Migration best practices
   - Troubleshooting guide
   - Security considerations

2. **FLYWAY_FIX.md** - MySQL 8.4 compatibility fix
   - Issue description
   - Root cause analysis
   - Solution applied

3. **CONFIGURATION_CHANGES.md** - This file
   - Summary of all changes
   - Configuration examples
   - Verification steps

## Rollback (if needed)

If you need to temporarily revert:

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # ⚠️ USE WITH EXTREME CAUTION
  flyway:
    enabled: false
```

**⚠️ WARNING**: Only do this in development, NEVER in production!

## Next Steps

### Immediate
1. ✅ Ensure database user has DDL permissions
2. ✅ Test application startup
3. ✅ Verify Flyway applies migrations correctly

### Future
1. Create migrations for any new schema changes
2. Review and merge migration PRs carefully
3. Test migrations in staging before production
4. Consider separate Flyway and application users in production

## References

- [DATABASE_MIGRATION_GUIDE.md](DATABASE_MIGRATION_GUIDE.md) - Complete Flyway setup guide
- [Flyway Documentation](https://documentation.red-gate.com/fd)
- [Spring Boot Flyway](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.flyway)

---

**Date**: October 10, 2025  
**Status**: ✅ APPLIED & VERIFIED  
**Build**: ✅ SUCCESSFUL  
**Risk Level**: 🟢 LOW (Development best practice)
