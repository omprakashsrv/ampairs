-- Supplier Module Database Migration Script (PostgreSQL)
-- Description: Create tenant-scoped supplier master data table (buy-side counterparty)
-- Mirrors the customer master on the sell side.

-- =====================================================
-- Enable PostGIS Extension (for the location Point column)
-- =====================================================
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE supplier
(
    id                 BIGSERIAL PRIMARY KEY,
    uid                VARCHAR(40)  NOT NULL UNIQUE,
    owner_id           VARCHAR(40)  NOT NULL,
    ref_id             VARCHAR(255),
    country_code       INT          NOT NULL,
    name               VARCHAR(255) NOT NULL,
    supplier_type      VARCHAR(100) NULL,
    supplier_group     VARCHAR(100) NULL,
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

CREATE INDEX idx_supplier_owner_name ON supplier (owner_id, name);
CREATE INDEX idx_supplier_owner_phone ON supplier (owner_id, phone);
CREATE INDEX idx_supplier_owner_email ON supplier (owner_id, email);
CREATE INDEX idx_supplier_status ON supplier (status);
CREATE INDEX idx_supplier_gst ON supplier (gst_number);

COMMENT ON TABLE supplier IS 'Workspace supplier master records (buy-side counterparty) with billing and shipping metadata';
