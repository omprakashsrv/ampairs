-- Business Module: Create businesses table
-- Feature: 003-business-module
-- Date: 2025-10-10
-- Description: Extract business profile and configuration data from workspaces table

CREATE TABLE businesses (
    -- Primary Key & Identity
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,

    -- Relationships (Multi-Tenancy via OwnableBaseDomain)
    owner_id VARCHAR(200) NOT NULL,  -- Workspace ID - @TenantId field from OwnableBaseDomain
    ref_id VARCHAR(255),

    -- Profile Information
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    description TEXT,
    owner_name VARCHAR(255),

    -- Address Information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    -- Location (GPS Coordinates)
    latitude DOUBLE,
    longitude DOUBLE,

    -- Contact Information
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
    operating_days JSON NOT NULL,  -- MySQL doesn't support default values; JPA provides default from entity

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit Timestamps (using TIMESTAMP for UTC storage)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    last_updated BIGINT NOT NULL DEFAULT 0,

    -- Foreign Key Constraints
    CONSTRAINT fk_business_workspace
        FOREIGN KEY (owner_id)
        REFERENCES workspaces(uid)
        ON DELETE CASCADE,

    -- Primary Key
    PRIMARY KEY (id)
);

-- Indexes for Performance
CREATE UNIQUE INDEX idx_business_owner ON businesses(owner_id);
CREATE UNIQUE INDEX ux_business_uid ON businesses(uid);
CREATE INDEX idx_business_ref_id ON businesses(ref_id);
CREATE INDEX idx_business_type ON businesses(business_type);
CREATE INDEX idx_business_active ON businesses(active);
CREATE INDEX idx_business_country ON businesses(country);
CREATE INDEX idx_business_created_at ON businesses(created_at);
