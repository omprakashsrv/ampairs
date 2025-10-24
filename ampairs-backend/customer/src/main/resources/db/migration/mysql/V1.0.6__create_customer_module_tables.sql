-- Customer Module Database Migration Script
-- Version: 4.3
-- Description: Create tenant-scoped customer master data tables
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_2__create_unit_module_tables.sql

-- =====================================================
-- Customer Groups
-- =====================================================
CREATE TABLE customer_groups
(
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    uid                         VARCHAR(40)  NOT NULL,
    owner_id                    VARCHAR(40)  NOT NULL,
    ref_id                      VARCHAR(255),
    group_code                  VARCHAR(20)  NOT NULL,
    name                        VARCHAR(100) NOT NULL,
    description                 VARCHAR(255),
    display_order               INT          NOT NULL DEFAULT 0,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    default_discount_percentage DOUBLE       NOT NULL DEFAULT 0.0,
    priority_level              INT          NOT NULL DEFAULT 0,
    metadata                    TEXT,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_customer_group_code_workspace (group_code, owner_id),
    INDEX idx_customer_group_name (name),
    INDEX idx_customer_group_active (active),
    INDEX idx_customer_group_workspace (owner_id),
    UNIQUE INDEX idx_customer_group_uid (uid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace-defined customer groupings with discount metadata';

-- =====================================================
-- Customer Types
-- =====================================================
CREATE TABLE customer_types
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    uid                  VARCHAR(40)  NOT NULL,
    owner_id             VARCHAR(40)  NOT NULL,
    ref_id               VARCHAR(255),
    type_code            VARCHAR(20)  NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    description          VARCHAR(255),
    display_order        INT          NOT NULL DEFAULT 0,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    default_credit_limit DOUBLE       NOT NULL DEFAULT 0.0,
    default_credit_days  INT          NOT NULL DEFAULT 0,
    metadata             TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_customer_type_code_workspace (type_code, owner_id),
    INDEX idx_customer_type_name (name),
    INDEX idx_customer_type_active (active),
    INDEX idx_customer_type_workspace (owner_id),
    UNIQUE INDEX idx_customer_type_uid (uid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace-defined customer categorisation master';

-- =====================================================
-- Master States
-- =====================================================
CREATE TABLE master_states
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    uid                 VARCHAR(40)  NOT NULL,
    state_code          VARCHAR(10)  NOT NULL,
    name                VARCHAR(100) NOT NULL,
    short_name          VARCHAR(10)  NOT NULL,
    country_code        VARCHAR(2)   NOT NULL,
    country_name        VARCHAR(100) NOT NULL,
    region              VARCHAR(100),
    timezone            VARCHAR(50),
    local_name          VARCHAR(100),
    capital             VARCHAR(100),
    population          BIGINT,
    area_sq_km          DOUBLE,
    gst_code            VARCHAR(2),
    postal_code_pattern VARCHAR(50),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    metadata            TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_master_state_uid (uid),
    UNIQUE INDEX idx_master_state_code (state_code),
    INDEX idx_master_state_country (country_code),
    INDEX idx_master_state_name (name),
    INDEX idx_master_state_active (active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Global catalogue of geographic states for import into workspaces';

-- =====================================================
-- Workspace States
-- =====================================================
CREATE TABLE state
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uid               VARCHAR(40)  NOT NULL,
    owner_id          VARCHAR(40)  NOT NULL,
    ref_id            VARCHAR(255),
    name              VARCHAR(100) NOT NULL,
    short_name        VARCHAR(6)   NOT NULL,
    country           VARCHAR(100) NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    master_state_id   VARCHAR(200),
    master_state_code VARCHAR(10),
    display_order     INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX state_name_idx (name, owner_id),
    INDEX state_master_idx (master_state_id),
    INDEX state_owner_idx (owner_id),
    UNIQUE INDEX idx_state_uid (uid),

    CONSTRAINT fk_state_master_state FOREIGN KEY (master_state_id) REFERENCES master_states (uid) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace-specific state list optionally linked to master state catalogue';

-- =====================================================
-- Customers
-- =====================================================
CREATE TABLE customer
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    uid                VARCHAR(40)  NOT NULL,
    owner_id           VARCHAR(40)  NOT NULL,
    ref_id             VARCHAR(255),
    country_code       INT          NOT NULL,
    name               VARCHAR(255) NOT NULL,
    customer_type      VARCHAR(100),
    customer_group     VARCHAR(100),
    phone              VARCHAR(20),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    landline           VARCHAR(12),
    email              VARCHAR(255),
    gst_number         VARCHAR(15),
    pan_number         VARCHAR(10),
    credit_limit       DOUBLE,
    credit_days        INT,
    outstanding_amount DOUBLE,
    address            VARCHAR(255),
    street             VARCHAR(255),
    street2            VARCHAR(255),
    city               VARCHAR(255),
    pincode            VARCHAR(10),
    state              VARCHAR(20),
    country            VARCHAR(20),
    location           POINT,
    billing_address    JSON,
    shipping_address   JSON,
    attributes         JSON,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_customer_uid (uid),
    INDEX idx_customer_owner_name (owner_id, name),
    INDEX idx_customer_owner_phone (owner_id, phone),
    INDEX idx_customer_owner_email (owner_id, email),
    INDEX idx_customer_status (status),
    INDEX idx_customer_gst (gst_number),
    UNIQUE INDEX uk_customer_gst (gst_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace customer master records with billing and shipping metadata';

-- =====================================================
-- Customer Images
-- =====================================================
CREATE TABLE customer_image
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uid               VARCHAR(40)  NOT NULL,
    owner_id          VARCHAR(40)  NOT NULL,
    ref_id            VARCHAR(255),
    customer_uid      VARCHAR(36)  NOT NULL,
    workspace_slug    VARCHAR(100) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_extension    VARCHAR(20)  NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    storage_path      VARCHAR(1000),
    storage_url       VARCHAR(1000),
    metadata          JSON COMMENT 'Image metadata: etag, lastModified, width, height, thumbnailsGenerated, additionalProperties',
    is_primary        BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order     INT          NOT NULL DEFAULT 0,
    description       VARCHAR(500),
    uploaded_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_customer_image_uid (uid),
    INDEX idx_customer_image_customer_uid (customer_uid),
    INDEX idx_customer_image_workspace (owner_id),
    INDEX idx_customer_image_primary (is_primary),
    INDEX idx_customer_image_created (created_at),
    INDEX idx_customer_image_content_type (content_type),

    CONSTRAINT fk_customer_images_customer FOREIGN KEY (customer_uid) REFERENCES customer (uid) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Customer imagery stored in object storage with workspace scoping and JSON metadata';
