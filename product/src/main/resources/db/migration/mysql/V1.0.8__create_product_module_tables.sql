-- Product Module Database Migration Script
-- Version: 4.4
-- Description: Create product catalog tables with imagery and pricing metadata
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_customer_module_tables.sql

-- =====================================================
-- Product Group
-- =====================================================
CREATE TABLE product_group
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    uid          VARCHAR(200) NOT NULL,
    owner_id     VARCHAR(200) NOT NULL,
    ref_id       VARCHAR(255),
    name         VARCHAR(255) NOT NULL,
    image_id     VARCHAR(200),
    index_no     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_group_uid (uid),
    INDEX idx_product_group_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace product group catalogue';

-- =====================================================
-- Product Brand
-- =====================================================
CREATE TABLE product_brand
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    uid          VARCHAR(200) NOT NULL,
    owner_id     VARCHAR(200) NOT NULL,
    ref_id       VARCHAR(255),
    name         VARCHAR(255) NOT NULL,
    image_id     VARCHAR(200),
    index_no     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_brand_uid (uid),
    INDEX idx_product_brand_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace product brand catalogue';

-- =====================================================
-- Product Category
-- =====================================================
CREATE TABLE product_category
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    uid          VARCHAR(200) NOT NULL,
    owner_id     VARCHAR(200) NOT NULL,
    ref_id       VARCHAR(255),
    name         VARCHAR(255) NOT NULL,
    image_id     VARCHAR(200),
    index_no     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_category_uid (uid),
    INDEX idx_product_category_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace product category catalogue';

-- =====================================================
-- Product Sub-Category
-- =====================================================
CREATE TABLE product_sub_category
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    uid          VARCHAR(200) NOT NULL,
    owner_id     VARCHAR(200) NOT NULL,
    ref_id       VARCHAR(255),
    name         VARCHAR(255) NOT NULL,
    image_id     VARCHAR(200),
    index_no     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_sub_category_uid (uid),
    INDEX idx_product_sub_category_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace product sub-category catalogue';

-- =====================================================
-- Product
-- =====================================================
CREATE TABLE product
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    uid             VARCHAR(200) NOT NULL,
    owner_id        VARCHAR(200) NOT NULL,
    ref_id          VARCHAR(255),
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(255) NOT NULL,
    sku             VARCHAR(100) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    tax_code_id     VARCHAR(36),
    tax_code        VARCHAR(20)  NOT NULL,
    unit_id         VARCHAR(36),
    base_price      DOUBLE       NOT NULL DEFAULT 0.0,
    cost_price      DOUBLE       NOT NULL DEFAULT 0.0,
    group_id        VARCHAR(200),
    brand_id        VARCHAR(200),
    category_id     VARCHAR(200),
    sub_category_id VARCHAR(200),
    base_unit_id    VARCHAR(200),
    mrp             DOUBLE       NOT NULL DEFAULT 0.0,
    dp              DOUBLE       NOT NULL DEFAULT 0.0,
    selling_price   DOUBLE       NOT NULL DEFAULT 0.0,
    index_no        INT          NOT NULL DEFAULT 0,
    attributes      JSON,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated    BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_uid (uid),
    UNIQUE INDEX idx_product_sku (sku),
    INDEX idx_product_owner_status (owner_id, status),
    INDEX idx_product_group (group_id),
    INDEX idx_product_category (category_id),
    INDEX idx_product_brand (brand_id),
    INDEX idx_product_owner_name (owner_id, name),

    CONSTRAINT fk_product_group FOREIGN KEY (group_id) REFERENCES product_group (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES product_brand (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (uid) ON DELETE SET NULL,
    CONSTRAINT fk_product_sub_category FOREIGN KEY (sub_category_id) REFERENCES product_sub_category (uid) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace product catalogue with pricing metadata';

-- Note: ProductImage uses VARCHAR(50) identifiers for product/image references.
--       Hibernate validates column length, so FK constraints to product/file tables
--       cannot be enforced here due to length mismatch (product.uid = VARCHAR(200)).
CREATE TABLE product_image
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    uid          VARCHAR(200) NOT NULL,
    owner_id     VARCHAR(200) NOT NULL,
    ref_id       VARCHAR(255),
    image_id     VARCHAR(50)  NOT NULL,
    product_id   VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_image_uid (uid),
    INDEX idx_product_image_product (product_id),
    INDEX idx_product_image_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace product imagery references';

-- =====================================================
-- Product Price
-- =====================================================
CREATE TABLE product_price
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uid           VARCHAR(200) NOT NULL,
    owner_id      VARCHAR(200) NOT NULL,
    ref_id        VARCHAR(255),
    product_id    VARCHAR(200) NOT NULL,
    mrp           DOUBLE       NOT NULL DEFAULT 0.0,
    dp            DOUBLE       NOT NULL DEFAULT 0.0,
    selling_price DOUBLE       NOT NULL DEFAULT 0.0,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated  BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_product_price_uid (uid),
    INDEX idx_product_price_product (product_id),
    INDEX idx_product_price_owner (owner_id),

    CONSTRAINT fk_product_price_product FOREIGN KEY (product_id) REFERENCES product (uid) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Per-product pricing snapshots';

-- =====================================================
-- Inventory Item
-- =====================================================
CREATE TABLE inventory
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uid           VARCHAR(200) NOT NULL,
    owner_id      VARCHAR(200) NOT NULL,
    ref_id        VARCHAR(255),
    description   VARCHAR(255) NOT NULL,
    product_id    VARCHAR(255) NOT NULL,
    stock         DOUBLE       NOT NULL DEFAULT 0.0,
    selling_price DOUBLE       NOT NULL DEFAULT 0.0,
    buying_price  DOUBLE       NOT NULL DEFAULT 0.0,
    mrp           DOUBLE       NOT NULL DEFAULT 0.0,
    dp            DOUBLE       NOT NULL DEFAULT 0.0,
    unit_id       VARCHAR(200),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated  BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_uid (uid),
    INDEX idx_inventory_product (product_id),
    INDEX idx_inventory_owner (owner_id),
    INDEX idx_inventory_unit (unit_id)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace stock ledger for products';

-- =====================================================
-- Inventory Transaction
-- =====================================================
CREATE TABLE inventory_transaction
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uid           VARCHAR(200) NOT NULL,
    owner_id      VARCHAR(200) NOT NULL,
    ref_id        VARCHAR(255),
    description   VARCHAR(255) NOT NULL,
    product_id    VARCHAR(255) NOT NULL,
    stock         DOUBLE       NOT NULL DEFAULT 0.0,
    selling_price DOUBLE       NOT NULL DEFAULT 0.0,
    mrp           DOUBLE       NOT NULL DEFAULT 0.0,
    dp            DOUBLE       NOT NULL DEFAULT 0.0,
    unit_id       VARCHAR(200),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated  BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_txn_uid (uid),
    INDEX idx_inventory_txn_product (product_id),
    INDEX idx_inventory_txn_owner (owner_id),
    INDEX idx_inventory_txn_unit (unit_id)


) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Historical inventory transactions for audit and reconciliation';

-- =====================================================
-- Inventory Unit Conversion
-- =====================================================
CREATE TABLE inventory_unit_conversion
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    uid             VARCHAR(200) NOT NULL,
    owner_id        VARCHAR(200) NOT NULL,
    ref_id          VARCHAR(255),
    base_unit_id    VARCHAR(200) NOT NULL,
    derived_unit_id VARCHAR(200) NOT NULL,
    inventory_id    VARCHAR(200) NOT NULL,
    multiplier      DOUBLE       NOT NULL DEFAULT 1.0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated    BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_unit_conv_uid (uid),
    INDEX idx_inventory_unit_conv_base (base_unit_id),
    INDEX idx_inventory_unit_conv_derived (derived_unit_id),
    INDEX idx_inventory_unit_conv_inventory (inventory_id),
    INDEX idx_inventory_unit_conv_owner (owner_id)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Unit conversion mapping for inventory records';
