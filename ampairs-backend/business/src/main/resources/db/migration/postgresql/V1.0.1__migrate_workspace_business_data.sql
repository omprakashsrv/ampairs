-- Business Module: Migrate business data from workspaces table (PostgreSQL)
-- Feature: 003-business-module
-- Date: 2025-10-10
-- Description: Copy business profile data from workspaces to businesses table

-- Insert business records from existing active workspaces
INSERT INTO businesses (
    uid,
    owner_id,
    name,
    business_type,
    description,
    address_line1,
    address_line2,
    city,
    state,
    postal_code,
    country,
    phone,
    email,
    website,
    tax_id,
    registration_number,
    timezone,
    currency,
    language,
    date_format,
    time_format,
    opening_hours,
    closing_hours,
    operating_days,
    active,
    created_at,
    updated_at,
    created_by
)
SELECT
    -- Generate new UID for business record (PostgreSQL version uses gen_random_uuid)
    CONCAT('bus_', SUBSTRING(MD5(CONCAT('business_', w.uid, NOW()::text)), 1, 16)) as uid,

    -- Relationship (owner_id is the workspace ID for multi-tenancy)
    w.uid as owner_id,

    -- Profile fields
    w.name as name,
    COALESCE(w.workspace_type, 'BUSINESS') as business_type,  -- Default to BUSINESS if null
    w.description as description,

    -- Address fields
    w.address_line1 as address_line1,
    w.address_line2 as address_line2,
    w.city as city,
    w.state as state,
    w.postal_code as postal_code,
    w.country as country,

    -- Contact fields
    w.phone as phone,
    w.email as email,
    w.website as website,

    -- Tax/Regulatory fields
    w.tax_id as tax_id,
    w.registration_number as registration_number,

    -- Operational configuration
    COALESCE(w.timezone, 'UTC') as timezone,
    COALESCE(w.currency, 'INR') as currency,
    COALESCE(w.language, 'en') as language,
    COALESCE(w.date_format, 'DD-MM-YYYY') as date_format,
    COALESCE(w.time_format, '12H') as time_format,

    -- Business hours
    w.business_hours_start as opening_hours,
    w.business_hours_end as closing_hours,

    -- Operating days (convert from JSON string if exists, otherwise default)
    COALESCE(w.working_days::jsonb, '["Monday","Tuesday","Wednesday","Thursday","Friday"]'::jsonb) as operating_days,

    -- Status
    w.active as active,

    -- Audit timestamps
    w.created_at as created_at,
    w.updated_at as updated_at,
    w.created_by as created_by

FROM workspaces w
WHERE w.active = TRUE  -- Only migrate active workspaces
AND NOT EXISTS (
    -- Prevent duplicate migration if script is run multiple times
    SELECT 1 FROM businesses b WHERE b.owner_id = w.uid
);

-- Log migration results
SELECT
    COUNT(*) as total_businesses_migrated,
    COUNT(DISTINCT owner_id) as unique_workspaces,
    MIN(created_at) as oldest_business,
    MAX(created_at) as newest_business
FROM businesses;
