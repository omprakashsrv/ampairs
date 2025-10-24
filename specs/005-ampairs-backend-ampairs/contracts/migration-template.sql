-- =====================================================
-- {Module Name} Database Migration Script
-- =====================================================
-- Version: {version}
-- Description: {description}
-- Author: {author}
-- Date: {date}
--
-- Tables Created:
-- 1. {table1} - {description1}
-- 2. {table2} - {description2}
--
-- Dependencies:
-- - Requires: {prerequisite_migrations}
-- - Required By: {dependent_migrations}
-- =====================================================

-- =====================================================
-- {Table 1 Name} Table
-- =====================================================
CREATE TABLE {table_name} (
    -- BaseDomain fields (ALL entities inherit these)
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    -- OwnableBaseDomain fields (if entity extends OwnableBaseDomain)
    owner_id VARCHAR(200) NOT NULL,  -- @TenantId for multi-tenancy
    ref_id VARCHAR(255),             -- External reference ID

    -- Entity-specific fields
    {column_name} {column_type} {nullable_constraint} {default_value},
    -- Example: name VARCHAR(255) NOT NULL,
    -- Example: email VARCHAR(255) NULL,
    -- Example: active BOOLEAN NOT NULL DEFAULT TRUE,
    -- Example: attributes JSON NULL,

    -- Primary Key and Indexes
    PRIMARY KEY (id),
    UNIQUE INDEX idx_{table}_uid (uid)
    -- Add additional indexes as needed:
    -- INDEX idx_{table}_{column} ({column_name}),
    -- UNIQUE INDEX uk_{table}_{column} ({column_name})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- {Table 2 Name} Table (with Foreign Keys)
-- =====================================================
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

    -- Foreign key column (defined before relationship annotation in JPA)
    {parent}_uid VARCHAR(200) NOT NULL,

    -- Entity-specific fields
    {column_definitions},

    -- Primary Key
    PRIMARY KEY (id),

    -- Indexes
    UNIQUE INDEX idx_{child_table}_uid (uid),
    INDEX idx_{child_table}_{parent} ({parent}_uid),

-- Foreign Keys
    FOREIGN KEY ({parent}_uid) REFERENCES {parent_table}(uid) ON DELETE CASCADE
    -- CASCADE options:
    -- ON DELETE CASCADE - Delete child when parent deleted (@OneToMany)
    -- ON DELETE SET NULL - Null reference when parent deleted (@OneToOne nullable)
    -- ON DELETE RESTRICT - Cannot delete parent if children exist (@ManyToOne)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- JSON Column Example
-- =====================================================
-- @Type(JsonType::class)
-- @Column(name = "attributes", columnDefinition = "json", nullable = false)
-- attributes JSON NOT NULL,

-- =====================================================
-- Spatial Column Example (Point)
-- =====================================================
-- @Column(name = "location")
-- location POINT NULL,
-- -- Optional: specify SRID if spatial indexes are required
-- -- SRID 4326 corresponds to WGS84 (lat/lon)
-- -- location POINT SRID 4326,

-- =====================================================
-- Self-referential Foreign Key Example
-- =====================================================
-- CREATE TABLE category (
--     id BIGINT NOT NULL AUTO_INCREMENT,
--    uid VARCHAR(200) NOT NULL,
--    owner_id VARCHAR(200) NOT NULL,
--    ref_id VARCHAR(255),
--    name VARCHAR(255) NOT NULL,
--    parent_uid VARCHAR(200),
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--    last_updated BIGINT NOT NULL DEFAULT 0,
--
--    PRIMARY KEY (id),
--    UNIQUE INDEX idx_category_uid (uid),
--    INDEX idx_category_parent (parent_uid),
--    CONSTRAINT fk_category_parent FOREIGN KEY (parent_uid) REFERENCES category(uid) ON DELETE SET NULL
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Example: Complete Unit Table
-- =====================================================
-- CREATE TABLE unit (
--     -- BaseDomain fields
--     id BIGINT NOT NULL AUTO_INCREMENT,
--     uid VARCHAR(200) NOT NULL UNIQUE,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     last_updated BIGINT NOT NULL DEFAULT 0,
--
--     -- OwnableBaseDomain fields
--     owner_id VARCHAR(200) NOT NULL,
--     ref_id VARCHAR(255),
--
--     -- Entity-specific fields
--     name VARCHAR(10) NOT NULL,
--     short_name VARCHAR(10) NOT NULL,
--     decimal_places INT NOT NULL DEFAULT 2,
--     active BOOLEAN NOT NULL DEFAULT TRUE,
--
--     PRIMARY KEY (id),
--     INDEX unit_idx (name),
--     UNIQUE INDEX idx_unit_uid (uid)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Notes
-- =====================================================
-- 1. TIMESTAMP vs DATETIME: Always use TIMESTAMP (UTC-aware)
-- 2. JSON columns: Use uppercase JSON (MySQL 8.0 native type)
-- 3. VARCHAR lengths: Must match @Column(length=...) in JPA entity
-- 4. NOT NULL: Must match nullable attribute in JPA @Column
-- 5. UNIQUE: Required for columns with @Column(unique = true)
-- 6. Foreign keys: Reference uid (VARCHAR(200)), NOT id (BIGINT)
-- 7. Indexes: Create on foreign keys and frequently queried columns
-- 8. Default values: Match JPA entity defaults
-- 9. ENGINE=InnoDB: Required for foreign key support
-- 10. CHARSET utf8mb4: Full Unicode support (emojis, special characters)
-- 11. CASCADE decision guide:
--     - Child lifecycle depends on parent (e.g., order items) → ON DELETE CASCADE
--     - Optional reference that should clear on delete (e.g., optional image) → ON DELETE SET NULL
--     - Shared reference (e.g., lookup tables) → ON DELETE RESTRICT
