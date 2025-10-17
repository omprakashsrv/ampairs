-- Business Module: Create businesses table (PostgreSQL)
-- Feature: 003-business-module
-- Date: 2025-10-10
-- Description: Extract business profile and configuration data from workspaces table

CREATE TABLE businesses
(
    -- Primary Key & Identity
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(40)  NOT NULL,

    -- Relationships (Multi-Tenancy via OwnableBaseDomain)
    owner_id            VARCHAR(40)  NOT NULL, -- Workspace ID - @TenantId field from OwnableBaseDomain
    ref_id              VARCHAR(100),

    -- Profile Information
    name                VARCHAR(255) NOT NULL,
    business_type       VARCHAR(20)  NOT NULL,
    description         TEXT,
    owner_name          VARCHAR(255),

    -- Address Information
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    postal_code         VARCHAR(20),
    country             VARCHAR(100),

    -- Location (GPS Coordinates)
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,

    -- Contact Information
    phone               VARCHAR(20),
    email               VARCHAR(255),
    website             VARCHAR(500),

    -- Tax & Regulatory
    tax_id              VARCHAR(50),
    registration_number VARCHAR(100),
    tax_settings        JSONB,

    -- Operational Configuration
    timezone            VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    currency            VARCHAR(3)   NOT NULL DEFAULT 'INR',
    language            VARCHAR(10)  NOT NULL DEFAULT 'en',
    date_format         VARCHAR(20)  NOT NULL DEFAULT 'DD-MM-YYYY',
    time_format         VARCHAR(10)  NOT NULL DEFAULT '12H',

    -- Business Hours
    opening_hours       VARCHAR(5),
    closing_hours       VARCHAR(5),
    operating_days      JSONB                 DEFAULT NULL,

    -- Status
    active              BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit Timestamps (TIMESTAMPTZ stores Instant values with timezone)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    last_updated        BIGINT       NOT NULL DEFAULT 0

);

-- Indexes for Performance
CREATE INDEX idx_business_type ON businesses (business_type);
CREATE INDEX idx_business_owner ON businesses (owner_id);
CREATE INDEX idx_business_active ON businesses (active) WHERE active = TRUE; -- Partial index for PostgreSQL
CREATE INDEX idx_business_country ON businesses (country);
CREATE INDEX idx_business_created_at ON businesses (created_at);
CREATE INDEX idx_business_ref_id ON businesses (ref_id);
CREATE UNIQUE INDEX ux_business_uid ON businesses (uid);

-- Comments for Documentation (PostgreSQL-specific)
COMMENT
    ON TABLE businesses IS 'Business profile and configuration data extracted from workspaces table for better separation of concerns';
COMMENT
    ON COLUMN businesses.owner_id IS 'Workspace ID (foreign key) - one business per workspace (1:1 relationship) - @TenantId field from OwnableBaseDomain';
COMMENT
    ON COLUMN businesses.uid IS 'Stable business identifier used by APIs (unique per workspace)';
COMMENT
    ON COLUMN businesses.business_type IS 'Type of business: RETAIL, WHOLESALE, MANUFACTURING, SERVICE, RESTAURANT, ECOMMERCE, HEALTHCARE, EDUCATION, REAL_ESTATE, LOGISTICS, OTHER';
COMMENT
    ON COLUMN businesses.ref_id IS 'External reference identifier (optional) for synchronization with client systems';
COMMENT
    ON COLUMN businesses.tax_settings IS 'JSONB field for tax-related settings by region (GST, VAT, etc.)';
COMMENT
    ON COLUMN businesses.operating_days IS 'JSONB array of operating days: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]';
COMMENT
    ON COLUMN businesses.timezone IS 'IANA timezone identifier (e.g., Asia/Kolkata, America/New_York)';
COMMENT
    ON COLUMN businesses.currency IS 'ISO 4217 currency code (e.g., INR, USD, EUR)';
COMMENT
    ON COLUMN businesses.created_at IS 'UTC timestamp when business was created';
COMMENT
    ON COLUMN businesses.updated_at IS 'UTC timestamp when business was last updated';
