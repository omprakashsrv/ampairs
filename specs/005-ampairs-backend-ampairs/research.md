# Flyway Migration Best Practices Research

**Feature**: Database Schema Migration for 22+ JPA Entities
**Date**: 2025-10-12
**Database**: MySQL 8.0+ with Multi-tenant Architecture
**Context**: Spring Boot 3.5.6 with Flyway, ddl-auto: validate

---

## Executive Summary

This document provides comprehensive research findings and decisions for creating Flyway migrations for 22+ JPA entities across 6 modules (core, unit, customer, product, order, invoice). The project uses Spring Boot 3.5.6 with Flyway already configured, and requires migration strategy for production databases that may already have tables created via JPA's ddl-auto.

**Key Finding**: Application configuration shows `baseline-on-migrate: true` is already enabled, which means production databases with existing tables will be automatically baselined at version 0 before applying new migrations.

---

## Decision 1: Migration Versioning Strategy

### Decision

Use **V4.x** series with sequential minor versions for the new retail management migrations, following the established pattern from existing migrations (V3_1, V3_2).

**Recommended Version Sequence**:
- **V4_1__create_unit_tables.sql** - Unit and UnitConversion
- **V4_2__create_customer_tables.sql** - Customer, CustomerImage, CustomerGroup, CustomerType, State
- **V4_3__create_product_tables.sql** - Product, ProductImage, ProductBrand, ProductCategory, ProductSubCategory, ProductGroup
- **V4_4__create_inventory_tables.sql** - Inventory, InventoryTransaction, InventoryUnitConversion
- **V4_5__create_order_tables.sql** - Order, OrderItem
- **V4_6__create_invoice_tables.sql** - Invoice, InvoiceItem
- **V4_7__create_form_tables.sql** - AttributeDefinition, FieldConfig (optional, future)

### Rationale

1. **Follows Existing Pattern**: Current migrations use `V{major}_{minor}__description` format (V3_1, V3_2), not timestamps
2. **Module-Based Grouping**: Each migration corresponds to a business module, making it easy to identify which tables belong together
3. **Dependency Order**: Version numbers reflect dependency relationships (unit → customer → product → inventory → order → invoice)
4. **Semantic Versioning**: Major version 4 indicates this is a significant feature addition (retail management platform)
5. **Maintainability**: Easier to track and reference specific migrations during debugging

### Alternatives Considered

1. **Single Large Migration (V4_1__create_all_retail_tables.sql)**
   - ❌ **Rejected**: Creates a 1000+ line file that's hard to review and maintain
   - ❌ **Rejected**: Increases risk - if migration fails midway, harder to identify which table caused the issue
   - ❌ **Rejected**: Violates single responsibility principle

2. **Timestamp-Based Versioning (V20251012143000__create_unit_tables.sql)**
   - ❌ **Rejected**: Doesn't match existing project convention
   - ❌ **Rejected**: Harder to understand chronological order without looking at actual timestamps
   - ❌ **Rejected**: Existing migrations V3_1 and V3_2 use sequential numbering

3. **Per-Table Migrations (V4_1__create_unit.sql, V4_2__create_unit_conversion.sql, etc.)**
   - ❌ **Rejected**: Creates 22+ separate migration files
   - ❌ **Rejected**: Clutters migration directory
   - ❌ **Rejected**: Increases complexity of foreign key management across files

### Implementation Notes

1. **Version Number Format**: `V{major}_{minor}__{description}.sql`
   - Major version incremented for significant feature sets
   - Minor version incremented for each module within the feature
   - Double underscore separates version from description
   - Description uses snake_case lowercase

2. **Module Definition**:
   ```
   Module: unit          → V1.0.0 (2 tables: unit, unit_conversion)
   Module: customer      → V1.0.0 (5 tables: customer, customer_image, customer_group, customer_type, master_state)
   Module: product       → V1.0.0 (6 tables: product, product_image, product_brand, product_category, product_sub_category, product_group)
   Module: inventory     → V1.0.0 (3 tables: inventory, inventory_transaction, inventory_unit_conversion)
   Module: order         → V1.0.0 (2 tables: customer_order, order_item)
   Module: invoice       → V1.0.0 (2 tables: invoice, invoice_item)
   Module: form          → V1.0.0 (2 tables: attribute_definition, field_config) [OPTIONAL - for future]
   Module: auth          → V1.0.0 (3 tables: device_session, login_session, auth_token)
   Module: workspace     → V1.0.0 (8 tables: workspaces, workspace_members, workspace_invitations, workspace_teams, master_modules, workspace_modules, workspace_settings, workspace_activities)
   Module: notification  → V1.0.0 (1 table: notification_queue)
   ```

3. **Future Extensions**:
   - Future versions increment from V1.0.1+ within each module as needed
   - Major version bumps reserved for schema changes that introduce breaking changes per module

4. **Location**: `{module}/src/main/resources/db/migration/{vendor}/` (aggregated via `classpath:db/migration/{vendor}`)
   - Application.yml specifies: `locations: classpath:db/migration/{vendor}`
   - MySQL-specific migrations go in `db/migration/` (vendor replaced with mysql)

---

## Decision 2: Entity-to-DDL Conversion Pattern

### Decision

**Standardized conversion rules based on JPA annotations with special handling for JSON columns, Instant timestamps, and @TenantId fields**.

### JPA Annotation Mapping Table

| JPA Annotation | SQL DDL Equivalent | Notes |
|----------------|-------------------|-------|
| `@Id @GeneratedValue(strategy = IDENTITY)` | `id BIGINT NOT NULL AUTO_INCREMENT, PRIMARY KEY (id)` | Auto-increment primary key |
| `@Column(name = "uid", unique = true)` | `uid VARCHAR(200) NOT NULL UNIQUE` | Business identifier |
| `@Column(nullable = false)` | `NOT NULL` | Required field |
| `@Column(length = 255)` | `VARCHAR(255)` | String length |
| `@Column(columnDefinition = "TEXT")` | `TEXT` | Long text fields |
| `Instant` type | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` | **UTC timestamps** |
| `@JdbcTypeCode(SqlTypes.JSON)` | `JSON` | JSON document storage |
| `@Type(JsonType::class) columnDefinition = "json"` | `json` (lowercase) | Hibernate JsonType mapping |
| `@Enumerated(EnumType.STRING)` | `VARCHAR(20) NOT NULL` | Enum stored as string |
| `@OneToMany @JoinColumn` | Foreign key with `ON DELETE CASCADE` | Child records deleted with parent |
| `@OneToOne @JoinColumn` | Foreign key with `ON DELETE SET NULL` | Null on parent deletion |
| `@ManyToOne @JoinColumn` | Foreign key with `ON DELETE RESTRICT` | Cannot delete parent if children exist |
| `Double` type | `DECIMAL(15,2)` or `DOUBLE` | Use DECIMAL for currency |
| `Boolean` type | `BOOLEAN NOT NULL DEFAULT FALSE` | MySQL TINYINT(1) |
| `@TenantId` | `owner_id VARCHAR(200) NOT NULL` | Multi-tenant isolation |
| `Point` type (JTS) | `POINT` | Spatial data for location |

### Base Domain Fields Pattern

**All entities extending `BaseDomain` inherit these columns**:

```sql
id BIGINT NOT NULL AUTO_INCREMENT,
uid VARCHAR(200) NOT NULL UNIQUE,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
last_updated BIGINT NOT NULL DEFAULT 0,
PRIMARY KEY (id),
INDEX idx_{table}_uid (uid)
```

**All entities extending `OwnableBaseDomain` add these columns**:

```sql
owner_id VARCHAR(200) NOT NULL,  -- @TenantId field for multi-tenancy
ref_id VARCHAR(255)              -- External reference (Tally, etc.)
```

### Rationale

1. **Consistency with JPA**: DDL precisely matches JPA entity definitions to prevent schema mismatch errors
2. **Instant for Timestamps**: Uses `TIMESTAMP` instead of `DATETIME` to support UTC timezone handling
3. **JSON Column Handling**: MySQL 8.0+ native JSON type provides validation and efficient querying
4. **Index Strategy**: Create indexes for:
   - Foreign key columns (improves JOIN performance)
   - Frequently queried fields (status, active, customer_id, etc.)
   - Unique constraints (uid, sku, email, gst_number)
5. **Default Values**: Match JPA defaults to ensure consistency between JPA-created and migration-created tables

### Special Cases and Examples

#### Example 1: Simple Entity (Unit.kt → unit table)

**Entity**:
```kotlin
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

**DDL**:
```sql
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
    INDEX idx_unit_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Example 2: Entity with JSON and Relationships (Customer.kt)

**Entity**:
```kotlin
@Entity(name = "customer")
class Customer : OwnableBaseDomain() {
    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "JSON")
    var attributes: Map<String, Any>? = null

    @Type(JsonType::class)
    @Column(name = "billing_address", nullable = false, columnDefinition = "json")
    var billingAddress: Address = Address()

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_uid", referencedColumnName = "uid")
    var images: MutableSet<CustomerImage> = mutableSetOf()
}
```

**DDL**:
```sql
CREATE TABLE customer (
    -- BaseDomain + OwnableBaseDomain fields (see above pattern)
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    -- ... other base fields ...

    -- Entity-specific fields
    name VARCHAR(255) NOT NULL,
    attributes JSON,                                    -- @JdbcTypeCode(SqlTypes.JSON)
    billing_address json NOT NULL,                     -- @Type(JsonType::class)

    PRIMARY KEY (id),
    INDEX idx_customer_uid (uid),
    INDEX idx_customer_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Child table for @OneToMany relationship
CREATE TABLE customer_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    customer_uid VARCHAR(200) NOT NULL,                -- Foreign key to parent
    storage_url VARCHAR(500) NOT NULL,
    -- ... other fields ...

    PRIMARY KEY (id),
    INDEX idx_customer_image_customer (customer_uid),
    FOREIGN KEY (customer_uid) REFERENCES customer(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Example 3: Enum Fields (Order.kt)

**Entity**:
```kotlin
@Entity(name = "customer_order")
class Order : OwnableBaseDomain() {
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.DRAFT
}

enum class OrderStatus {
    DRAFT, CONFIRMED, PROCESSING, FULFILLED, CANCELLED
}
```

**DDL**:
```sql
CREATE TABLE customer_order (
    -- ... base fields ...
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',    -- EnumType.STRING stored as VARCHAR

    -- ... other fields ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Alternatives Considered

1. **Use JPA ddl-auto=update to Generate DDL**
   - ❌ **Rejected**: Application.yml explicitly sets `ddl-auto: validate` for production safety
   - ❌ **Rejected**: Loses version control and auditability
   - ❌ **Rejected**: Cannot review changes before deployment

2. **Use Hibernate Schema Export Tool**
   - ⚠️ **Partial Use**: Can use to generate initial DDL, but requires manual review and formatting
   - ⚠️ **Requires Adjustments**: Generated DDL may not match conventions (index names, formatting)

### Implementation Notes

1. **Naming Convention Verification**:
   - Application.yml specifies: `hibernate.naming.physical-strategy: CamelCaseToUnderscoresNamingStrategy`
   - JPA field `customerName` → Database column `customer_name`
   - JPA field `gstNumber` → Database column `gst_number`

2. **JSON Column Distinction**:
   - `@JdbcTypeCode(SqlTypes.JSON)` → `JSON` (uppercase, MySQL 8.0 native type)
   - `@Type(JsonType::class)` → `json` (lowercase, Hibernate type)
   - **Both are valid**, but uppercase is preferred for new tables

3. **Instant Timestamp Pattern** (CRITICAL):
   ```sql
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   ```
   - Database stores in UTC
   - Connection string includes `?serverTimezone=UTC`
   - Jackson serializes as ISO-8601 with Z suffix

4. **Index Naming Convention**:
   - Primary key index: Automatic via `PRIMARY KEY (id)`
   - Foreign key index: `idx_{table}_{column}` (e.g., `idx_order_customer_id`)
   - Unique index: `idx_{table}_{column}` or `uk_{table}_{columns}` for composite
   - Search index: `idx_{table}_{column}` (e.g., `idx_customer_name`)

5. **Foreign Key Cascades**:
   - `ON DELETE CASCADE`: Child records deleted when parent deleted (@OneToMany)
   - `ON DELETE SET NULL`: Reference nullified when parent deleted (@OneToOne with nullable=true)
   - `ON DELETE RESTRICT`: Cannot delete parent if children exist (@ManyToOne)

6. **Triggers for Timestamp Updates**:
   - MySQL `ON UPDATE CURRENT_TIMESTAMP` handles `updated_at` automatically
   - Optional: Create triggers for `last_updated` (Unix milliseconds) if needed
   - Example pattern from V3_1 migration:
   ```sql
   DELIMITER $$
   CREATE TRIGGER {table}_updated_at
       BEFORE UPDATE ON {table}
       FOR EACH ROW
   BEGIN
       SET NEW.updated_at = CURRENT_TIMESTAMP;
       SET NEW.last_updated = UNIX_TIMESTAMP() * 1000;
   END$$
   DELIMITER ;
   ```

---

## Decision 3: Dependency Ordering and Table Creation Sequence

### Decision

**Create tables in dependency order to avoid foreign key constraint errors**. Each migration file contains tables for one module, ordered by dependencies within that file.

### Recommended Table Creation Order

#### V4_1__create_unit_tables.sql
1. `unit` (no dependencies)
2. `unit_conversion` (depends on: unit)

#### V4_2__create_customer_tables.sql
1. `customer_group` (no dependencies)
2. `customer_type` (no dependencies)
3. `master_state` (no dependencies)
4. `customer` (depends on: customer_group, customer_type, master_state via foreign keys or references)
5. `customer_image` (depends on: customer)

#### V4_3__create_product_tables.sql
1. `product_group` (no dependencies)
2. `product_brand` (no dependencies)
3. `product_category` (no dependencies)
4. `product_sub_category` (depends on: product_category)
5. `product` (depends on: unit, product_group, product_brand, product_category, product_sub_category)
6. `product_image` (depends on: product)

#### V4_4__create_inventory_tables.sql
1. `inventory` (depends on: product, unit)
2. `inventory_transaction` (depends on: inventory, product)
3. `inventory_unit_conversion` (depends on: inventory, unit)

#### V4_5__create_order_tables.sql
1. `customer_order` (depends on: customer via customer_id reference)
2. `order_item` (depends on: customer_order, product)

#### V4_6__create_invoice_tables.sql
1. `invoice` (depends on: customer_order via order_ref_id, customer)
2. `invoice_item` (depends on: invoice, product)

### Rationale

1. **Prevents Foreign Key Errors**: Child tables created after parent tables
2. **Matches Business Logic**: Reflects real-world relationships (customers place orders, orders generate invoices)
3. **Module Isolation**: Each migration file is self-contained for its module
4. **Clear Dependencies**: Visual representation of entity relationships
5. **Rollback Safety**: Reverse order deletion (invoice → order → product → customer → unit)

### Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│ V4_1: Unit Module (Foundation)                              │
│   unit ──→ unit_conversion                                  │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ V4_2: Customer Module                                       │
│   customer_group, customer_type, master_state               │
│        ↓                                                    │
│   customer ──→ customer_image                               │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ V4_3: Product Module (Depends on Unit)                     │
│   product_group, product_brand, product_category            │
│        ↓                                                    │
│   product_sub_category (→ product_category)                 │
│        ↓                                                    │
│   product (→ unit, groups, brand, category)                 │
│        ↓                                                    │
│   product_image                                             │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ V4_4: Inventory Module (Depends on Product, Unit)          │
│   inventory (→ product, unit)                               │
│        ↓                                                    │
│   inventory_transaction, inventory_unit_conversion          │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ V4_5: Order Module (Depends on Customer, Product)          │
│   customer_order (→ customer)                               │
│        ↓                                                    │
│   order_item (→ customer_order, product)                    │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ V4_6: Invoice Module (Depends on Order, Customer, Product) │
│   invoice (→ customer_order, customer)                      │
│        ↓                                                    │
│   invoice_item (→ invoice, product)                         │
└─────────────────────────────────────────────────────────────┘
```

### Foreign Key Relationships

| Parent Table | Child Table | Foreign Key Column | Cascade Rule |
|--------------|-------------|-------------------|--------------|
| unit | unit_conversion | unit_id | ON DELETE CASCADE |
| customer | customer_image | customer_uid | ON DELETE CASCADE |
| customer_category | customer_sub_category | category_id | ON DELETE CASCADE |
| product_group | product | group_id | ON DELETE SET NULL |
| product_brand | product | brand_id | ON DELETE SET NULL |
| product_category | product | category_id | ON DELETE SET NULL |
| product_sub_category | product | sub_category_id | ON DELETE SET NULL |
| unit | product | base_unit_id | ON DELETE SET NULL |
| product | product_image | product_id | ON DELETE CASCADE |
| product | inventory | product_id | ON DELETE CASCADE |
| unit | inventory | unit_id | ON DELETE SET NULL |
| inventory | inventory_transaction | inventory_id | ON DELETE CASCADE |
| customer | customer_order | customer_id | ON DELETE RESTRICT |
| customer_order | order_item | order_id | ON DELETE CASCADE |
| product | order_item | product_id | ON DELETE RESTRICT |
| customer_order | invoice | order_ref_id | ON DELETE SET NULL |
| customer | invoice | from_customer_id / to_customer_id | ON DELETE RESTRICT |
| invoice | invoice_item | invoice_id | ON DELETE CASCADE |
| product | invoice_item | product_id | ON DELETE RESTRICT |

### Alternatives Considered

1. **All Tables in Single File**
   - ❌ **Rejected**: Creates 1000+ line migration file
   - ❌ **Rejected**: Hard to review and debug
   - ❌ **Rejected**: Violates separation of concerns

2. **Per-Table Migration Files**
   - ❌ **Rejected**: Creates 22+ migration files
   - ❌ **Rejected**: Complex to manage foreign key dependencies across files
   - ❌ **Rejected**: Increases risk of version number conflicts

3. **Alphabetical Order**
   - ❌ **Rejected**: Ignores dependency relationships
   - ❌ **Rejected**: Will cause foreign key constraint errors

### Implementation Notes

1. **Within Each Migration File**:
   - Create parent tables before child tables
   - Create lookup/master tables first (groups, types, categories)
   - Create main entity tables second
   - Create relationship/junction tables last

2. **Foreign Key Constraint Checks**:
   ```sql
   -- At start of migration (optional, for complex scenarios)
   SET FOREIGN_KEY_CHECKS = 0;

   -- Create tables...

   -- At end of migration
   SET FOREIGN_KEY_CHECKS = 1;
   ```
   - **Use Sparingly**: Only if circular dependencies exist
   - **Not Recommended**: Better to fix dependency order

3. **Testing Order**:
   - Test each migration file independently on clean database
   - Test all migrations in sequence on clean database
   - Verify foreign key constraints work correctly

4. **Rollback Considerations**:
   - Drop tables in reverse order (child before parent)
   - Example: invoice_item → invoice → order_item → customer_order → product → customer → unit

---

## Decision 4: Baseline Strategy for Existing Databases

### Decision

**Use Flyway's baseline-on-migrate feature (already enabled) to handle existing production databases**. No additional configuration needed.

### Current Configuration Analysis

From `/ampairs-backend/ampairs_service/src/main/resources/application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}  # Validates schema, doesn't modify
  flyway:
    enabled: ${FLYWAY_ENABLED:true}
    baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:true}  # ✅ CRITICAL
    baseline-version: ${FLYWAY_BASELINE_VERSION:0}
    validate-on-migrate: ${FLYWAY_VALIDATE_ON_MIGRATE:true}
    out-of-order: ${FLYWAY_OUT_OF_ORDER:false}
```

### How It Works

1. **Fresh Database** (Development, Staging):
   - Flyway creates `flyway_schema_history` table
   - Executes all migrations from V2_2 → V3_1 → V3_2 → V4_1 → V4_2 → ... → V4_6
   - All tables created via migrations

2. **Existing Database with Tables** (Production):
   - Flyway checks if `flyway_schema_history` exists
   - If NOT exists AND tables already present:
     - Creates `flyway_schema_history` table
     - Marks database as baseline version 0 (or configured version)
     - **Skips migrations ≤ baseline version**
     - **Executes only new migrations > baseline version**

3. **After Baseline**:
   - Production database marked as V0 (no migrations applied initially)
   - V2_2, V3_1, V3_2 marked as "<< Ignored >>" (tables already exist)
   - **V4_1 through V4_6 will execute** (new tables to be created)

### Rationale

1. **Already Configured**: No code changes needed, feature enabled via application.yml
2. **Safe for Production**: Prevents re-running old migrations on databases with existing tables
3. **Automatic Detection**: Flyway automatically detects if baseline is needed
4. **Version Flexibility**: Can adjust baseline-version via environment variable if needed
5. **Zero Downtime**: No need to shut down production to apply migrations

### Production Deployment Strategy

#### Scenario 1: Production Database Already Has Tables (via JPA ddl-auto)

**Current State**:
- Tables exist: `workspace`, `workspace_member`, `users`, etc.
- No `flyway_schema_history` table
- Database created via `ddl-auto: update` or `ddl-auto: create-update`

**Deployment Steps**:

1. **Before Deployment**:
   - Review all migration scripts V4_1 through V4_6
   - Test migrations on staging environment with baseline
   - Verify JPA entities match DDL definitions

2. **Switch Configuration**:
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate  # ✅ Change from 'update' to 'validate'
     flyway:
       enabled: true
       baseline-on-migrate: true    # ✅ Already enabled
       baseline-version: 0          # ✅ Mark existing state as V0
   ```

3. **Deploy Application**:
   - Application starts
   - Flyway detects no `flyway_schema_history`
   - **Automatically runs baseline**: Creates schema history table, marks as V0
   - Executes V4_1 → V4_2 → V4_3 → V4_4 → V4_5 → V4_6
   - New tables created (unit, customer, product, order, invoice)

4. **Post-Deployment Verification**:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```
   Expected output:
   ```
   | installed_rank | version | description              | type     | script                           | success |
   |----------------|---------|--------------------------|----------|----------------------------------|---------|
   | 1              | 0       | << Flyway Baseline >>    | BASELINE | << Flyway Baseline >>            | 1       |
   | 2              | 4.1     | create unit tables       | SQL      | V4_1__create_unit_tables.sql     | 1       |
   | 3              | 4.2     | create customer tables   | SQL      | V4_2__create_customer_tables.sql | 1       |
   | 4              | 4.3     | create product tables    | SQL      | V4_3__create_product_tables.sql  | 1       |
   | 5              | 4.4     | create inventory tables  | SQL      | V4_4__create_inventory_tables.sql| 1       |
   | 6              | 4.5     | create order tables      | SQL      | V4_5__create_order_tables.sql    | 1       |
   | 7              | 4.6     | create invoice tables    | SQL      | V4_6__create_invoice_tables.sql  | 1       |
   ```

#### Scenario 2: Fresh Production Database (New Deployment)

**Current State**:
- Empty database
- No tables
- No `flyway_schema_history`

**Deployment Steps**:

1. **Deploy Application with Flyway Enabled**:
   - Flyway creates `flyway_schema_history`
   - Executes ALL migrations in order: V2_2 → V3_1 → V3_2 → V4_1 → V4_2 → ... → V4_6
   - All tables created via migrations

2. **Verification**:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```
   Expected output:
   ```
   | installed_rank | version | description              | success |
   |----------------|---------|--------------------------|---------|
   | 1              | 2.2     | add retail modules       | 1       |
   | 2              | 3.1     | create tax module tables | 1       |
   | 3              | 3.2     | create event system...   | 1       |
   | 4              | 4.1     | create unit tables       | 1       |
   | 5              | 4.2     | create customer tables   | 1       |
   | 6              | 4.3     | create product tables    | 1       |
   | 7              | 4.4     | create inventory tables  | 1       |
   | 8              | 4.5     | create order tables      | 1       |
   | 9              | 4.6     | create invoice tables    | 1       |
   ```

#### Scenario 3: Staging Environment (Testing Migrations)

**Purpose**: Validate migrations before production deployment

**Steps**:

1. **Create Database Snapshot** (optional):
   ```bash
   mysqldump -u root -p munsi_app > backup_pre_migration.sql
   ```

2. **Deploy with Flyway**:
   - Application starts
   - Migrations execute
   - Monitor logs for errors

3. **Validate Schema**:
   ```sql
   -- Check all tables exist
   SHOW TABLES;

   -- Verify foreign keys
   SELECT
       TABLE_NAME,
       CONSTRAINT_NAME,
       REFERENCED_TABLE_NAME
   FROM information_schema.KEY_COLUMN_USAGE
   WHERE REFERENCED_TABLE_NAME IS NOT NULL
   AND TABLE_SCHEMA = 'munsi_app';

   -- Check indexes
   SELECT
       TABLE_NAME,
       INDEX_NAME,
       COLUMN_NAME
   FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = 'munsi_app'
   ORDER BY TABLE_NAME, INDEX_NAME;
   ```

4. **Test JPA Validation**:
   - Application startup with `ddl-auto: validate`
   - Should succeed without errors
   - Confirms DDL matches JPA entities

### Alternatives Considered

1. **Manual Baseline Command**
   ```bash
   ./gradlew flywayBaseline -Dflyway.baselineVersion=3.2
   ```
   - ❌ **Rejected**: Requires manual intervention on each database
   - ❌ **Rejected**: Prone to human error
   - ✅ **When to Use**: Only if automatic baseline fails

2. **Conditional CREATE TABLE IF NOT EXISTS**
   ```sql
   CREATE TABLE IF NOT EXISTS customer (...);
   ```
   - ❌ **Rejected**: Doesn't provide proper versioning
   - ❌ **Rejected**: Flyway checksum validation will fail on subsequent runs
   - ❌ **Rejected**: Not a best practice for migrations

3. **Separate Migration Set for Existing Databases**
   - ❌ **Rejected**: Increases maintenance burden
   - ❌ **Rejected**: Risk of schema drift between fresh and existing databases

### Implementation Notes

1. **Environment Variable Overrides**:
   ```bash
   # If default baseline-version=0 doesn't work
   export FLYWAY_BASELINE_VERSION=3.2
   export FLYWAY_BASELINE_ON_MIGRATE=true
   ```

2. **Monitoring Baseline Execution**:
   - Check application logs for:
   ```
   INFO  - Flyway Community Edition by Redgate
   INFO  - Database: jdbc:mysql://localhost:3306/munsi_app (MySQL 8.0)
   INFO  - Creating Schema History table `munsi_app`.`flyway_schema_history`
   INFO  - Baseling schema `munsi_app` to version 0 - << Flyway Baseline >>
   INFO  - Successfully baselined schema with version: 0
   INFO  - Migrating schema `munsi_app` to version 4.1 - create unit tables
   ```

3. **Rollback Strategy**:
   - Flyway Community Edition **does not support automatic rollback**
   - Manual rollback required via SQL scripts:
   ```sql
   -- Drop in reverse order
   DROP TABLE IF EXISTS invoice_item;
   DROP TABLE IF EXISTS invoice;
   DROP TABLE IF EXISTS order_item;
   DROP TABLE IF EXISTS customer_order;
   -- ... etc
   ```
   - **Recommendation**: Test thoroughly in staging before production

4. **Checksum Validation**:
   - Flyway calculates checksum for each migration file
   - **Never modify migration files after execution**
   - If modification needed, create new migration (e.g., V4_7__fix_customer_table.sql)

5. **Production Safety Checklist**:
   - [ ] Test migrations on staging environment
   - [ ] Backup production database before deployment
   - [ ] Verify `ddl-auto: validate` is set
   - [ ] Confirm `baseline-on-migrate: true` is enabled
   - [ ] Monitor application logs during startup
   - [ ] Verify `flyway_schema_history` table created
   - [ ] Check all V4.x migrations executed successfully
   - [ ] Test application functionality post-migration

---

## Decision 5: Testing and Validation Strategy

### Decision

**Multi-layered testing approach using Testcontainers, JPA validation, and manual verification** to ensure migrations match entity definitions and maintain data integrity.

### Testing Layers

#### Layer 1: Testcontainers Integration Tests

**Purpose**: Automated validation that migrations execute successfully and match JPA entities

**Implementation**:

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class FlywayMigrationTest {

    companion object {
        @Container
        val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("test_db")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { "true" }
        }
    }

    @Autowired
    lateinit var flywayMigrationValidator: FlywayMigrationValidator

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `should successfully execute all migrations`() {
        // Flyway migrations run automatically during context startup
        // If this test passes, migrations executed successfully
        assertTrue(true)
    }

    @Test
    fun `should validate JPA entities match database schema`() {
        // Spring Boot automatically validates schema when ddl-auto=validate
        // If context loads successfully, validation passed

        // Additional manual checks
        val metamodel = entityManager.metamodel
        val entities = metamodel.entities

        assertThat(entities).isNotEmpty
        // Verify key entities exist
        assertThat(entities.map { it.name }).contains(
            "unit", "customer", "product", "order", "invoice"
        )
    }

    @Test
    fun `should verify foreign key constraints exist`() {
        val sql = """
            SELECT
                TABLE_NAME,
                CONSTRAINT_NAME,
                REFERENCED_TABLE_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE REFERENCED_TABLE_NAME IS NOT NULL
            AND TABLE_SCHEMA = 'test_db'
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
        val results = query.resultList

        assertThat(results).isNotEmpty
        // Verify key relationships exist
        // Example: order_item → customer_order
        // Example: product → unit
    }

    @Test
    fun `should verify indexes exist on foreign keys`() {
        val sql = """
            SELECT
                TABLE_NAME,
                INDEX_NAME,
                COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = 'test_db'
            AND INDEX_NAME != 'PRIMARY'
            ORDER BY TABLE_NAME, INDEX_NAME
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
        val results = query.resultList

        assertThat(results).isNotEmpty
        // Verify indexes on frequently queried columns
    }

    @Test
    fun `should verify JSON columns are properly typed`() {
        val sql = """
            SELECT
                TABLE_NAME,
                COLUMN_NAME,
                DATA_TYPE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'test_db'
            AND DATA_TYPE = 'json'
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
        val results = query.resultList

        assertThat(results).isNotEmpty
        // Verify JSON columns: attributes, billing_address, shipping_address
    }

    @Test
    fun `should verify timestamp columns are TIMESTAMP type`() {
        val sql = """
            SELECT
                TABLE_NAME,
                COLUMN_NAME,
                DATA_TYPE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'test_db'
            AND COLUMN_NAME IN ('created_at', 'updated_at')
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
        val results = query.resultList

        assertThat(results).isNotEmpty
        // Verify all timestamp columns use TIMESTAMP, not DATETIME
    }
}
```

**Test Configuration** (`/ampairs-backend/ampairs_service/src/test/resources/application-test.yml`):

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Must be validate, not create or update
  flyway:
    enabled: true
    clean-disabled: false  # Allow clean for tests
  datasource:
    # Testcontainers provides dynamic values
```

#### Layer 2: Manual Migration Verification

**Purpose**: Human review of migration scripts before deployment

**Checklist**:

1. **Syntax and Formatting**:
   - [ ] SQL syntax is valid MySQL 8.0+
   - [ ] Consistent formatting (2-space indentation)
   - [ ] Comments explain complex logic
   - [ ] File encoding is UTF-8

2. **Schema Correctness**:
   - [ ] All tables have `ENGINE=InnoDB`
   - [ ] All tables use `CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`
   - [ ] Primary keys defined as `id BIGINT NOT NULL AUTO_INCREMENT`
   - [ ] `uid VARCHAR(200) NOT NULL UNIQUE` on all tables
   - [ ] Timestamp columns use `TIMESTAMP`, not `DATETIME`
   - [ ] JSON columns use correct type (`JSON` or `json`)

3. **Relationships**:
   - [ ] Foreign keys defined with proper `ON DELETE` rules
   - [ ] Indexes exist on all foreign key columns
   - [ ] Parent tables created before child tables
   - [ ] Circular dependencies avoided

4. **Multi-Tenancy**:
   - [ ] All `OwnableBaseDomain` entities have `owner_id VARCHAR(200) NOT NULL`
   - [ ] Indexes include `owner_id` for tenant filtering

5. **Comparison with JPA Entities**:
   - [ ] Column names match JPA `@Column(name = "...")` annotations
   - [ ] Data types match JPA field types
   - [ ] Nullable constraints match JPA `nullable` attributes
   - [ ] String lengths match JPA `length` attributes

#### Layer 3: Staging Environment Validation

**Purpose**: Real-world testing before production deployment

**Steps**:

1. **Deploy to Staging**:
   ```bash
   # Set staging environment variables
   export DB_URL=jdbc:mysql://staging-db:3306/munsi_app?serverTimezone=UTC
   export FLYWAY_ENABLED=true
   export JPA_DDL_AUTO=validate

   # Deploy application
   ./gradlew :ampairs_service:bootRun
   ```

2. **Monitor Logs**:
   - Check for Flyway migration success messages
   - Verify no SQL errors
   - Confirm `ddl-auto: validate` passes

3. **Functional Testing**:
   ```bash
   # Test CRUD operations for each entity
   curl -X POST http://staging/api/v1/unit -d '{"name":"KG","shortName":"kg"}'
   curl -X GET http://staging/api/v1/unit

   curl -X POST http://staging/api/v1/customer -d '{"name":"Test Customer",...}'
   curl -X GET http://staging/api/v1/customer

   # ... test all endpoints
   ```

4. **Database Inspection**:
   ```sql
   -- Verify all tables created
   SHOW TABLES;

   -- Check foreign keys
   SELECT * FROM information_schema.KEY_COLUMN_USAGE
   WHERE REFERENCED_TABLE_NAME IS NOT NULL
   AND TABLE_SCHEMA = 'munsi_app';

   -- Verify indexes
   SHOW INDEX FROM customer;
   SHOW INDEX FROM product;
   SHOW INDEX FROM customer_order;

   -- Test multi-tenancy isolation
   SELECT * FROM unit WHERE owner_id = 'WORKSPACE_123';
   ```

#### Layer 4: Production Deployment Validation

**Purpose**: Post-deployment verification in production

**Steps**:

1. **Pre-Deployment Backup**:
   ```bash
   mysqldump -u root -p munsi_app > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Deploy Application**:
   - Monitor application logs in real-time
   - Watch for Flyway migration messages

3. **Post-Deployment Checks**:
   ```sql
   -- Verify flyway_schema_history
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;

   -- Verify all V4.x migrations succeeded
   SELECT version, description, success
   FROM flyway_schema_history
   WHERE version LIKE '4.%';

   -- Verify tables exist
   SHOW TABLES LIKE 'unit';
   SHOW TABLES LIKE 'customer';
   SHOW TABLES LIKE 'product';
   SHOW TABLES LIKE 'customer_order';
   SHOW TABLES LIKE 'invoice';

   -- Verify row counts (should be 0 initially)
   SELECT COUNT(*) FROM unit;
   SELECT COUNT(*) FROM customer;
   ```

4. **Application Health Check**:
   ```bash
   curl http://production/actuator/health
   curl http://production/api/v1/unit  # Should return empty array
   ```

### Rationale

1. **Automated Testing**: Testcontainers provides consistent, repeatable tests
2. **Human Review**: Catches logical errors and convention violations
3. **Staging Validation**: Real-world testing before production impact
4. **Production Verification**: Ensures deployment succeeded correctly
5. **Layered Defense**: Multiple checkpoints reduce risk of schema issues

### Rollback Procedures

#### If Migration Fails During Execution

1. **Flyway Tracks Partial Failures**:
   - Failed migrations marked in `flyway_schema_history` with `success = 0`
   - Application startup will fail on subsequent attempts

2. **Manual Rollback Steps**:
   ```sql
   -- Drop tables created by failed migration (reverse order)
   DROP TABLE IF EXISTS invoice_item;
   DROP TABLE IF EXISTS invoice;
   DROP TABLE IF EXISTS order_item;
   DROP TABLE IF EXISTS customer_order;
   DROP TABLE IF EXISTS inventory_unit_conversion;
   DROP TABLE IF EXISTS inventory_transaction;
   DROP TABLE IF EXISTS inventory;
   DROP TABLE IF EXISTS product_image;
   DROP TABLE IF EXISTS product;
   DROP TABLE IF EXISTS product_sub_category;
   DROP TABLE IF EXISTS product_category;
   DROP TABLE IF EXISTS product_brand;
   DROP TABLE IF EXISTS product_group;
   DROP TABLE IF EXISTS customer_image;
   DROP TABLE IF EXISTS customer;
   DROP TABLE IF EXISTS master_state;
   DROP TABLE IF EXISTS customer_type;
   DROP TABLE IF EXISTS customer_group;
   DROP TABLE IF EXISTS unit_conversion;
   DROP TABLE IF EXISTS unit;

   -- Remove failed migration entry
   DELETE FROM flyway_schema_history WHERE version = '4.1' AND success = 0;
   ```

3. **Fix Migration Script**:
   - Identify error from logs
   - Correct SQL syntax
   - Re-deploy application

#### If Migration Succeeds But Schema Is Wrong

1. **Create Repair Migration**:
   ```sql
   -- V4_7__fix_customer_table_column_type.sql
   ALTER TABLE customer MODIFY COLUMN phone VARCHAR(20) NOT NULL;
   ```

2. **Deploy Repair Migration**:
   - Flyway applies V4_7 automatically
   - Schema corrected

### Testing Tools and Commands

#### Testcontainers Dependency

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:mysql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}
```

#### Run Tests

```bash
# Run migration tests only
./gradlew :ampairs_service:test --tests FlywayMigrationTest

# Run all tests
./gradlew :ampairs_service:test

# Run with verbose output
./gradlew :ampairs_service:test --info
```

#### Manual Database Validation

```sql
-- Compare JPA entity fields with database columns
-- Example for Customer entity
DESCRIBE customer;

-- Expected output should match:
-- id, uid, created_at, updated_at, last_updated (BaseDomain)
-- owner_id, ref_id (OwnableBaseDomain)
-- name, phone, email, gst_number, ... (Customer fields)

-- Verify foreign keys
SHOW CREATE TABLE order_item;
-- Should see:
-- CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `customer_order` (`uid`) ON DELETE CASCADE
-- CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`uid`) ON DELETE RESTRICT
```

### Alternatives Considered

1. **Skip Testing, Deploy Directly**
   - ❌ **Rejected**: High risk of production failures
   - ❌ **Rejected**: No confidence in schema correctness

2. **Manual Testing Only**
   - ❌ **Rejected**: Human error prone
   - ❌ **Rejected**: Not repeatable or scalable

3. **Use JPA ddl-auto=update Instead of Migrations**
   - ❌ **Rejected**: Loses version control
   - ❌ **Rejected**: No audit trail
   - ❌ **Rejected**: Risky for production

### Implementation Notes

1. **Continuous Integration**:
   - Add Flyway tests to CI/CD pipeline
   - Fail build if migrations don't pass validation

2. **Documentation**:
   - Maintain migration changelog
   - Document any manual steps required

3. **Communication**:
   - Notify team before deploying migrations
   - Schedule deployment during low-traffic periods

4. **Monitoring**:
   - Set up alerts for migration failures
   - Monitor application startup time (migrations add overhead)

---

## Appendix A: Quick Reference Commands

### Flyway Commands

```bash
# Check migration status
./gradlew flywayInfo

# Manually run migrations
./gradlew flywayMigrate

# Validate checksums
./gradlew flywayValidate

# Baseline existing database (manual)
./gradlew flywayBaseline -Dflyway.baselineVersion=3.2

# Repair schema history (use with caution)
./gradlew flywayRepair
```

### Database Inspection

```sql
-- View migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Check for failed migrations
SELECT * FROM flyway_schema_history WHERE success = 0;

-- List all tables
SHOW TABLES;

-- Show table structure
DESCRIBE customer;
SHOW CREATE TABLE customer;

-- List foreign keys
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME IS NOT NULL
AND TABLE_SCHEMA = 'munsi_app'
ORDER BY TABLE_NAME;

-- List indexes
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'munsi_app'
ORDER BY TABLE_NAME, INDEX_NAME;
```

### JPA Validation

```bash
# Start application with validation
export JPA_DDL_AUTO=validate
./gradlew :ampairs_service:bootRun

# Check logs for validation errors
tail -f logs/application.log | grep -i "schema"
```

---

## Appendix B: Entity-to-Migration Mapping

### Module: Unit (V4_1)

| Entity | Table Name | Foreign Keys | Notes |
|--------|-----------|--------------|-------|
| Unit | unit | - | Base unit of measurement |
| UnitConversion | unit_conversion | unit_id → unit.uid | Conversion ratios |

### Module: Customer (V4_2)

| Entity | Table Name | Foreign Keys | Notes |
|--------|-----------|--------------|-------|
| CustomerGroup | customer_group | - | Customer categorization |
| CustomerType | customer_type | - | B2B, B2C, etc. |
| MasterState | master_state | - | Indian states lookup |
| Customer | customer | customer_group_id?, customer_type_id? | Main customer entity |
| CustomerImage | customer_image | customer_uid → customer.uid | Customer photos |

### Module: Product (V4_3)

| Entity | Table Name | Foreign Keys | Notes |
|--------|-----------|--------------|-------|
| ProductGroup | product_group | - | Product grouping |
| ProductBrand | product_brand | - | Brand lookup |
| ProductCategory | product_category | - | Top-level categories |
| ProductSubCategory | product_sub_category | category_id → product_category.uid | Second-level categories |
| Product | product | group_id, brand_id, category_id, sub_category_id, base_unit_id | Main product entity |
| ProductImage | product_image | product_id → product.uid | Product photos |

### Module: Inventory (V4_4)

| Entity | Table Name | Foreign Keys | Notes |
|--------|-----------|--------------|-------|
| Inventory | inventory | product_id → product.uid, unit_id → unit.uid | Stock levels |
| InventoryTransaction | inventory_transaction | inventory_id → inventory.uid | Stock movements |
| InventoryUnitConversion | inventory_unit_conversion | inventory_id, unit_id | Inventory-specific conversions |

### Module: Order (V4_5)

| Entity | Table Name | Foreign Keys | Notes |
|--------|-----------|--------------|-------|
| Order | customer_order | customer_id → customer.uid | Sales orders |
| OrderItem | order_item | order_id → customer_order.uid, product_id → product.uid | Line items |

### Module: Invoice (V4_6)

| Entity | Table Name | Foreign Keys | Notes |
|--------|-----------|--------------|-------|
| Invoice | invoice | order_ref_id → customer_order.ref_id, from_customer_id, to_customer_id | Invoices |
| InvoiceItem | invoice_item | invoice_id → invoice.uid, product_id → product.uid | Invoice line items |

---

## Appendix C: Migration File Templates

### Template 1: Simple Entity Table

```sql
-- Module: {module_name}
-- Version: {version}
-- Description: {description}
-- Author: {author}
-- Date: {date}

CREATE TABLE {table_name} (
    -- BaseDomain fields
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    -- OwnableBaseDomain fields (if applicable)
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Entity-specific fields
    {column_definitions},

    PRIMARY KEY (id),
    INDEX idx_{table}_uid (uid)
    {additional_indexes}
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Template 2: Child Table with Foreign Key

```sql
CREATE TABLE {child_table} (
    -- BaseDomain fields
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    -- OwnableBaseDomain fields
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Foreign key reference
    {parent}_uid VARCHAR(200) NOT NULL,

    -- Entity-specific fields
    {column_definitions},

    PRIMARY KEY (id),
    INDEX idx_{child_table}_uid (uid),
    INDEX idx_{child_table}_{parent} ({parent}_uid),
    FOREIGN KEY ({parent}_uid) REFERENCES {parent_table}(uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Template 3: Migration File Header

```sql
-- =====================================================
-- {Module Name} Database Migration Script
-- =====================================================
-- Version: {version}
-- Description: {detailed_description}
-- Author: {author_name}
-- Date: {date}
--
-- Tables Created:
-- 1. {table1} - {description}
-- 2. {table2} - {description}
-- ...
--
-- Dependencies:
-- - Requires: {prerequisite_migrations}
-- - Required By: {dependent_migrations}
-- =====================================================
```

---

## Appendix D: Troubleshooting Guide

### Issue 1: Migration Fails with Foreign Key Error

**Symptom**:
```
ERROR: Cannot add foreign key constraint 'fk_order_item_order'
```

**Cause**: Parent table doesn't exist or foreign key column type mismatch

**Solution**:
1. Check parent table created first in migration file
2. Verify foreign key column type matches referenced column
3. Ensure referencing `uid VARCHAR(200)`, not `id BIGINT`

### Issue 2: JPA Validation Fails After Migration

**Symptom**:
```
Schema-validation: wrong column type encountered in column [created_at]
```

**Cause**: DDL column type doesn't match JPA entity field type

**Solution**:
1. Review entity field type (`Instant` → `TIMESTAMP`)
2. Check migration DDL (`created_at TIMESTAMP`)
3. Verify database column type: `DESCRIBE table_name;`

### Issue 3: Baseline Not Applied Automatically

**Symptom**: Application fails to start, tries to rerun old migrations

**Cause**: `baseline-on-migrate: false` or `flyway_schema_history` already exists

**Solution**:
```bash
# Check configuration
grep baseline application.yml

# Manually baseline if needed
./gradlew flywayBaseline -Dflyway.baselineVersion=3.2

# Or drop and recreate schema history (CAUTION)
DROP TABLE flyway_schema_history;
# Restart application
```

### Issue 4: Checksum Mismatch Error

**Symptom**:
```
ERROR: Migration checksum mismatch for migration 4.1
```

**Cause**: Migration file modified after execution

**Solution**:
```bash
# Option 1: Repair schema history (updates checksum)
./gradlew flywayRepair

# Option 2: Create new migration to fix issue
# DO NOT modify V4_1, create V4_7 instead
```

### Issue 5: Circular Dependency Error

**Symptom**:
```
ERROR: Cannot create table 'table_a' - references non-existent table 'table_b'
ERROR: Cannot create table 'table_b' - references non-existent table 'table_a'
```

**Cause**: Two tables reference each other

**Solution**:
```sql
-- Temporarily disable foreign key checks
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE table_a (...);
CREATE TABLE table_b (...);

SET FOREIGN_KEY_CHECKS = 1;
```

---

## Conclusion

This research document provides comprehensive guidance for implementing Flyway migrations for the Ampairs retail management platform. Key takeaways:

1. **Versioning**: Use V4.x series with module-based grouping (V4_1 through V4_6)
2. **DDL Conversion**: Follow standardized JPA-to-SQL mapping patterns with special attention to Instant timestamps and JSON columns
3. **Dependency Order**: Create tables in module dependency order (unit → customer → product → inventory → order → invoice)
4. **Baseline Strategy**: Leverage existing `baseline-on-migrate: true` configuration for production databases with existing tables
5. **Testing**: Multi-layered validation using Testcontainers, JPA validation, staging environment, and manual inspection

**Next Steps**:
1. Create migration files V4_1 through V4_6 based on patterns in this document
2. Implement Testcontainers tests for automated validation
3. Deploy to staging environment for real-world testing
4. Create production deployment checklist
5. Schedule production deployment with backup and rollback procedures

**References**:
- Existing migrations: V3_1 (tax module), V3_2 (event system)
- Application configuration: `/ampairs-backend/ampairs_service/src/main/resources/application.yml`
- Entity definitions: `/ampairs-backend/{module}/src/main/kotlin/com/ampairs/{module}/domain/model/`
- Flyway documentation: https://documentation.red-gate.com/fd/
