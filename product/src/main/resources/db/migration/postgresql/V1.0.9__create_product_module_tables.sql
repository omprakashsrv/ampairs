-- Product Module Database Migration Script (PostgreSQL)
-- Version: 4.4
-- Description: Create product catalog tables with imagery and pricing metadata
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_customer_module_tables.sql

-- =====================================================
-- Product Group
-- =====================================================
CREATE TABLE product_group (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    image_id VARCHAR(200),
    index_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_group_image FOREIGN KEY (image_id) REFERENCES file (uid) ON DELETE SET NULL
);

CREATE INDEX idx_product_group_owner ON product_group(owner_id);
COMMENT ON TABLE product_group IS 'Workspace product group catalogue';

-- =====================================================
-- Product Brand
-- =====================================================
CREATE TABLE product_brand (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    image_id VARCHAR(200),
    index_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_brand_image FOREIGN KEY (image_id) REFERENCES file (uid) ON DELETE SET NULL
);

CREATE INDEX idx_product_brand_owner ON product_brand(owner_id);
COMMENT ON TABLE product_brand IS 'Workspace product brand catalogue';

-- =====================================================
-- Product Category
-- =====================================================
CREATE TABLE product_category (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    image_id VARCHAR(200),
    index_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_category_image FOREIGN KEY (image_id) REFERENCES file (uid) ON DELETE SET NULL
);

CREATE INDEX idx_product_category_owner ON product_category(owner_id);
COMMENT ON TABLE product_category IS 'Workspace product category catalogue';

-- =====================================================
-- Product Sub-Category
-- =====================================================
CREATE TABLE product_sub_category (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    image_id VARCHAR(200),
    index_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_sub_category_image FOREIGN KEY (image_id) REFERENCES file (uid) ON DELETE SET NULL
);

CREATE INDEX idx_product_sub_category_owner ON product_sub_category(owner_id);
COMMENT ON TABLE product_sub_category IS 'Workspace product sub-category catalogue';

-- =====================================================
-- Product
-- =====================================================
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tax_code_id VARCHAR(36),
    tax_code VARCHAR(20) NOT NULL,
    unit_id VARCHAR(36),
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    cost_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    group_id VARCHAR(200),
    brand_id VARCHAR(200),
    category_id VARCHAR(200),
    sub_category_id VARCHAR(200),
    base_unit_id VARCHAR(200),
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    attributes JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_group FOREIGN KEY (group_id) REFERENCES product_group (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES product_brand (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_sub_category FOREIGN KEY (sub_category_id) REFERENCES product_sub_category (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_unit FOREIGN KEY (base_unit_id) REFERENCES unit (uid) ON DELETE SET NULL
);

CREATE INDEX idx_product_owner_status ON product(owner_id, status);
CREATE INDEX idx_product_group ON product(group_id);
CREATE INDEX idx_product_category ON product(category_id);
CREATE INDEX idx_product_brand ON product(brand_id);
CREATE INDEX idx_product_owner_name ON product(owner_id, name);
COMMENT ON TABLE product IS 'Workspace product catalogue with pricing metadata';

-- Note: ProductImage uses VARCHAR(50) identifiers for product/image references.
--       Hibernate validates column length, so FK constraints to product/file tables
--       cannot be enforced here due to length mismatch (product.uid = VARCHAR(200)).
CREATE TABLE product_image (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    image_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_image_product ON product_image(product_id);
CREATE INDEX idx_product_image_owner ON product_image(owner_id);
COMMENT ON TABLE product_image IS 'Workspace product imagery references';

-- =====================================================
-- Product Price
-- =====================================================
CREATE TABLE product_price (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    product_id VARCHAR(200) NOT NULL,
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_price_product FOREIGN KEY (product_id) REFERENCES product (uid) ON DELETE CASCADE
);

CREATE INDEX idx_product_price_product ON product_price(product_id);
CREATE INDEX idx_product_price_owner ON product_price(owner_id);
COMMENT ON TABLE product_price IS 'Per-product pricing snapshots';

-- =====================================================
-- Inventory Item
-- =====================================================
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    stock DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    buying_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    unit_id VARCHAR(200),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_unit FOREIGN KEY (unit_id) REFERENCES unit (uid) ON DELETE SET NULL
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_owner ON inventory(owner_id);
CREATE INDEX idx_inventory_unit ON inventory(unit_id);
COMMENT ON TABLE inventory IS 'Workspace stock ledger for products';

-- =====================================================
-- Inventory Transaction
-- =====================================================
CREATE TABLE inventory_transaction (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    stock DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    unit_id VARCHAR(200),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_txn_unit FOREIGN KEY (unit_id) REFERENCES unit (uid) ON DELETE SET NULL
);

CREATE INDEX idx_inventory_txn_product ON inventory_transaction(product_id);
CREATE INDEX idx_inventory_txn_owner ON inventory_transaction(owner_id);
CREATE INDEX idx_inventory_txn_unit ON inventory_transaction(unit_id);
COMMENT ON TABLE inventory_transaction IS 'Historical inventory transactions for audit and reconciliation';

-- =====================================================
-- Inventory Unit Conversion
-- =====================================================
CREATE TABLE inventory_unit_conversion (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    base_unit_id VARCHAR(200) NOT NULL,
    derived_unit_id VARCHAR(200) NOT NULL,
    inventory_id VARCHAR(200) NOT NULL,
    multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_unit_conv_base FOREIGN KEY (base_unit_id) REFERENCES unit (uid) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_unit_conv_derived FOREIGN KEY (derived_unit_id) REFERENCES unit (uid) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_unit_conv_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (uid) ON DELETE CASCADE
);

CREATE INDEX idx_inventory_unit_conv_base ON inventory_unit_conversion(base_unit_id);
CREATE INDEX idx_inventory_unit_conv_derived ON inventory_unit_conversion(derived_unit_id);
CREATE INDEX idx_inventory_unit_conv_inventory ON inventory_unit_conversion(inventory_id);
CREATE INDEX idx_inventory_unit_conv_owner ON inventory_unit_conversion(owner_id);
COMMENT ON TABLE inventory_unit_conversion IS 'Unit conversion mapping for inventory records';
