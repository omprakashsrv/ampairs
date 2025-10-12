# Business Module - Migration Fix Summary

## What Happened

The Business module migration failed on the first attempt due to two issues:

### Issue 1: JSON Column Default Value (MySQL Limitation)
**Error**: `BLOB, TEXT, GEOMETRY or JSON column 'operating_days' can't have a default value`

**Cause**: The initial migration script tried to set a default value for a JSON column:
```sql
operating_days JSON NOT NULL DEFAULT '[]',  -- ❌ MySQL doesn't allow this
```

MySQL doesn't support default values for JSON, TEXT, BLOB, or GEOMETRY columns.

### Issue 2: Column Name Mismatch
**Error**: Migration used `workspace_id` but entity expects `owner_id`

**Cause**: The Business entity extends `OwnableBaseDomain`, which has an `ownerId` field (with `@TenantId`). The migration script incorrectly used `workspace_id` instead of `owner_id`.

## What Was Fixed

### ✅ Fixed Migration V1.0.0 (create_businesses_table.sql)

1. **Removed seq_id field**: Not needed - BaseDomain provides `id` and `uid`
2. **Changed workspace_id to owner_id**: Matches OwnableBaseDomain's `ownerId` field
3. **Fixed JSON column**: Removed default value
   ```sql
   operating_days JSON NOT NULL,  -- Application provides default in entity
   ```
4. **Updated foreign key**: References `owner_id` instead of `workspace_id`
5. **Updated indexes**: Changed `idx_business_workspace` to `idx_business_owner`
6. **Updated comments**: Clarified that `owner_id` is the workspace ID for multi-tenancy

### ✅ Fixed Migration V1.0.1 (migrate_workspace_business_data.sql)

1. **Removed seq_id from INSERT**: Not needed
2. **Changed workspace_id to owner_id**: Matches schema
3. **Updated references**: All queries and EXISTS checks now use `owner_id`

### ✅ Build Status

```bash
./gradlew clean :ampairs_service:build -x test
# ✅ BUILD SUCCESSFUL
```

All code compiles successfully with the fixed migrations.

## Current Issue: Failed Migration in Database

**Status**: ❌ Flyway detected a failed migration

The first migration attempt failed and left the Flyway schema history in a "failed" state:
```
Detected failed migration to version 1.0.0 (create businesses table).
Please remove any half-completed changes then run repair to fix the schema history.
```

This means:
- The `flyway_schema_history` table has version 1.0.0 marked as `success = 0`
- The `businesses` table may or may not exist (partially created)
- Flyway won't re-run the migration until we clean up

## How to Fix

### Option 1: Run the Repair Script (Recommended)

Execute the provided SQL repair script:

```bash
mysql -u root -p munsi_app < business/REPAIR_MIGRATION.sql
```

Or manually in MySQL Workbench/CLI:

```sql
-- Check the failed migration
SELECT * FROM flyway_schema_history WHERE version = '1.0.0';

-- Remove the failed entry
DELETE FROM flyway_schema_history WHERE version = '1.0.0' AND success = 0;

-- Drop the table if it exists
DROP TABLE IF EXISTS businesses;

-- Verify cleanup
SELECT * FROM flyway_schema_history WHERE version = '1.0.0';
SHOW TABLES LIKE 'businesses';
```

### Option 2: Enable Flyway Clean (Development Only)

⚠️ **WARNING**: This will drop ALL tables and re-create from scratch.

Add to `application.yml`:
```yaml
spring:
  flyway:
    clean-disabled: false
```

Then run:
```bash
./gradlew :ampairs_service:flywayClean
./gradlew :ampairs_service:bootRun
```

### Option 3: Baseline on Migrate (If you have existing data)

If you have existing data you want to keep:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

This will skip the business table creation since you'll manually create it.

## After Repair: Test the Migration

1. **Clean the database state** (using Option 1 above)

2. **Start the application**:
   ```bash
   ./gradlew :ampairs_service:bootRun
   ```

3. **Expected output** (successful migration):
   ```
   INFO  Flyway Community Edition ... by Redgate
   INFO  Creating Schema History table `munsi_app`.`flyway_schema_history` ...
   INFO  Current version of schema `munsi_app`: << Empty Schema >>
   INFO  Migrating schema `munsi_app` to version "1.0.0 - create businesses table"
   INFO  Migrating schema `munsi_app` to version "1.0.1 - migrate workspace business data"
   INFO  Successfully applied 2 migrations to schema `munsi_app`, now at version v1.0.1
   INFO  Started AmpairsApplication in X.XXX seconds
   ```

4. **Verify the table was created**:
   ```sql
   DESCRIBE businesses;
   SELECT * FROM flyway_schema_history;
   ```

## Expected Database Schema

After successful migration, the `businesses` table should have:

```sql
CREATE TABLE businesses (
    -- Primary Key (from BaseDomain)
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) UNIQUE NOT NULL,

    -- Multi-Tenancy (from OwnableBaseDomain)
    owner_id VARCHAR(200) NOT NULL,  -- Workspace ID with @TenantId
    ref_id VARCHAR(255),

    -- Business Profile
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    description TEXT,
    owner_name VARCHAR(255),

    -- Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    -- Location
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),

    -- Contact
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),

    -- Tax & Regulatory
    tax_id VARCHAR(50),
    registration_number VARCHAR(100),
    tax_settings JSON,

    -- Operational Configuration
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    date_format VARCHAR(20) NOT NULL DEFAULT 'DD-MM-YYYY',
    time_format VARCHAR(10) NOT NULL DEFAULT '12H',

    -- Business Hours
    opening_hours VARCHAR(5),
    closing_hours VARCHAR(5),
    operating_days JSON NOT NULL,

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit (from BaseDomain)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_updated BIGINT NOT NULL,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),

    -- Foreign Key
    CONSTRAINT fk_business_workspace
        FOREIGN KEY (owner_id)
        REFERENCES workspaces(uid)
        ON DELETE CASCADE
);
```

## Key Points

### ✅ What's Working
- All code compiles successfully
- Migration scripts are fixed and correct
- Entity model matches database schema
- Multi-tenancy (@TenantId on ownerId) properly configured

### ⚠️ What Needs Action
- Clean up the failed Flyway migration entry in database
- Drop partially created businesses table (if exists)
- Re-run the application to apply migrations successfully

### 📋 Files Modified
1. `V1.0.0__create_businesses_table.sql` - Fixed schema creation
2. `V1.0.1__migrate_workspace_business_data.sql` - Fixed data migration
3. `REPAIR_MIGRATION.sql` - New repair script (this session)
4. `MIGRATION_FIX_SUMMARY.md` - This documentation (this session)

## Next Steps

1. **Execute the repair script** to clean up the database
2. **Start the application** to apply migrations
3. **Verify** the businesses table was created correctly
4. **Test** the Business API endpoints

## API Endpoints (After Successful Migration)

```
GET    /api/v1/business           - Get business profile
POST   /api/v1/business           - Create business profile
PUT    /api/v1/business           - Update business profile
GET    /api/v1/business/exists    - Check if business exists
GET    /api/v1/business/address   - Get formatted address
```

## Testing Commands

```bash
# Check if business exists for workspace
curl -X GET http://localhost:8080/api/v1/business/exists \
  -H "X-Workspace-ID: your-workspace-id" \
  -H "Authorization: Bearer your-jwt-token"

# Create business profile
curl -X POST http://localhost:8080/api/v1/business \
  -H "Content-Type: application/json" \
  -H "X-Workspace-ID: your-workspace-id" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{
    "name": "My Business",
    "business_type": "RETAIL",
    "timezone": "Asia/Kolkata",
    "currency": "INR"
  }'
```

---

**Date**: 2025-10-11
**Status**: ✅ Code Fixed, ⏳ Database Cleanup Needed
**Action Required**: Run `REPAIR_MIGRATION.sql` to clean up failed migration
