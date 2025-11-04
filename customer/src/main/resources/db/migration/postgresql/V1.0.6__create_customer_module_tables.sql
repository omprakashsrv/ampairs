-- Customer Module Database Migration Script (PostgreSQL)
-- Version: 4.3
-- Description: Create tenant-scoped customer master data tables
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_2__create_unit_module_tables.sql

-- =====================================================
-- Enable PostGIS Extension
-- =====================================================
CREATE EXTENSION IF NOT EXISTS postgis;

-- =====================================================
-- Customer Groups
-- =====================================================
CREATE TABLE customer_groups
(
    id                          BIGSERIAL PRIMARY KEY,
    uid                         VARCHAR(40)      NOT NULL UNIQUE,
    owner_id                    VARCHAR(40)      NOT NULL,
    ref_id                      VARCHAR(255),
    group_code                  VARCHAR(20)      NOT NULL,
    name                        VARCHAR(100)     NOT NULL,
    description                 VARCHAR(255),
    display_order               INT              NOT NULL DEFAULT 0,
    active                      BOOLEAN          NOT NULL DEFAULT TRUE,
    default_discount_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    priority_level              INT              NOT NULL DEFAULT 0,
    metadata                    TEXT,
    created_at                  TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated                BIGINT           NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_customer_group_code_workspace ON customer_groups (group_code, owner_id);
CREATE INDEX idx_customer_group_name ON customer_groups (name);
CREATE INDEX idx_customer_group_active ON customer_groups (active);
CREATE INDEX idx_customer_group_workspace ON customer_groups (owner_id);

COMMENT
ON TABLE customer_groups IS 'Workspace-defined customer groupings with discount metadata';

-- =====================================================
-- Customer Types
-- =====================================================
CREATE TABLE customer_types
(
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(40)      NOT NULL UNIQUE,
    owner_id             VARCHAR(40)      NOT NULL,
    ref_id               VARCHAR(255),
    type_code            VARCHAR(20)      NOT NULL,
    name                 VARCHAR(100)     NOT NULL,
    description          VARCHAR(255),
    display_order        INT              NOT NULL DEFAULT 0,
    active               BOOLEAN          NOT NULL DEFAULT TRUE,
    default_credit_limit DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    default_credit_days  INT              NOT NULL DEFAULT 0,
    metadata             TEXT,
    created_at           TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated         BIGINT           NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_customer_type_code_workspace ON customer_types (type_code, owner_id);
CREATE INDEX idx_customer_type_name ON customer_types (name);
CREATE INDEX idx_customer_type_active ON customer_types (active);
CREATE INDEX idx_customer_type_workspace ON customer_types (owner_id);

COMMENT
ON TABLE customer_types IS 'Workspace-defined customer categorisation master';

-- =====================================================
-- Master States
-- =====================================================
CREATE TABLE master_states
(
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(200) NOT NULL UNIQUE,
    state_code          VARCHAR(10)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    short_name          VARCHAR(10)  NOT NULL,
    country_code        VARCHAR(2)   NOT NULL,
    country_name        VARCHAR(100) NOT NULL,
    region              VARCHAR(100),
    timezone            VARCHAR(50),
    local_name          VARCHAR(100),
    capital             VARCHAR(100),
    population          BIGINT,
    area_sq_km          DOUBLE PRECISION,
    gst_code            VARCHAR(2),
    postal_code_pattern VARCHAR(50),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    metadata            TEXT,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated        BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_master_state_country ON master_states (country_code);
CREATE INDEX idx_master_state_name ON master_states (name);
CREATE INDEX idx_master_state_active ON master_states (active);

COMMENT
ON TABLE master_states IS 'Global catalogue of geographic states for import into workspaces';

-- =====================================================
-- Workspace States
-- =====================================================
CREATE TABLE state
(
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(40)  NOT NULL UNIQUE,
    owner_id          VARCHAR(40)  NOT NULL,
    ref_id            VARCHAR(255),
    name              VARCHAR(100) NOT NULL,
    short_name        VARCHAR(6)   NOT NULL,
    country           VARCHAR(100) NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    master_state_id   VARCHAR(200),
    master_state_code VARCHAR(10),
    display_order     INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_state_master_state FOREIGN KEY (master_state_id) REFERENCES master_states (uid) ON DELETE SET NULL
);

CREATE UNIQUE INDEX state_name_idx ON state (name, owner_id);
CREATE INDEX state_master_idx ON state (master_state_id);
CREATE INDEX state_owner_idx ON state (owner_id);

COMMENT
ON TABLE state IS 'Workspace-specific state list optionally linked to master state catalogue';

-- =====================================================
-- Customers
-- =====================================================
CREATE TABLE customer
(
    id                 BIGSERIAL PRIMARY KEY,
    uid                VARCHAR(40)  NOT NULL UNIQUE,
    owner_id           VARCHAR(40)  NOT NULL,
    ref_id             VARCHAR(255),
    country_code       INT          NOT NULL,
    name               VARCHAR(255) NOT NULL,
    customer_type      VARCHAR(100) NULL,
    customer_group     VARCHAR(100) NULL,
    phone              VARCHAR(20) NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    landline           VARCHAR(12) NULL,
    email              VARCHAR(255) NULL,
    gst_number         VARCHAR(15),
    pan_number         VARCHAR(10),
    credit_limit       DOUBLE PRECISION NULL,
    credit_days        INT NULL,
    outstanding_amount DOUBLE PRECISION NULL,
    address            VARCHAR(255) NULL,
    street             VARCHAR(255) NULL,
    street2            VARCHAR(255) NULL,
    city               VARCHAR(255) NULL,
    pincode            VARCHAR(10) NULL,
    state              VARCHAR(20) NULL,
    country            VARCHAR(20) NULL,
    location           GEOMETRY(Point, 4326),
    billing_address    JSONB NULL,
    shipping_address   JSONB NULL,
    attributes         JSONB,
    created_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_owner_name ON customer (owner_id, name);
CREATE INDEX idx_customer_owner_phone ON customer (owner_id, phone);
CREATE INDEX idx_customer_owner_email ON customer (owner_id, email);
CREATE INDEX idx_customer_status ON customer (status);
CREATE INDEX idx_customer_gst ON customer (gst_number);
CREATE UNIQUE INDEX uk_customer_gst ON customer (gst_number);

COMMENT
ON TABLE customer IS 'Workspace customer master records with billing and shipping metadata';

-- =====================================================
-- Customer Images
-- =====================================================
CREATE TABLE customer_image
(
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(200) NOT NULL UNIQUE,
    owner_id          VARCHAR(200) NOT NULL,
    ref_id            VARCHAR(255),
    customer_uid      VARCHAR(36)  NOT NULL,
    workspace_slug    VARCHAR(100) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_extension    VARCHAR(20)  NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    storage_path      VARCHAR(1000),
    storage_url       VARCHAR(1000),
    metadata          JSONB        NULL,
    is_primary        BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order     INT          NOT NULL DEFAULT 0,
    description       VARCHAR(500),
    uploaded_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_images_customer FOREIGN KEY (customer_uid) REFERENCES customer (uid) ON DELETE CASCADE
);

CREATE INDEX idx_customer_image_customer_uid ON customer_image (customer_uid);
CREATE INDEX idx_customer_image_workspace ON customer_image (owner_id);
CREATE INDEX idx_customer_image_primary ON customer_image (is_primary);
CREATE INDEX idx_customer_image_created ON customer_image (created_at);
CREATE INDEX idx_customer_image_content_type ON customer_image (content_type);

-- GIN index for efficient JSON querying
CREATE INDEX idx_customer_image_metadata ON customer_image USING GIN (metadata);

COMMENT ON TABLE customer_image IS 'Customer imagery stored in object storage with workspace scoping and JSONB metadata';
COMMENT ON COLUMN customer_image.metadata IS 'Image metadata: etag, lastModified, width, height, thumbnailsGenerated, additionalProperties';
