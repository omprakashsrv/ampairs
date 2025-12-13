-- Tax Module V2 Database Migration Script (PostgreSQL)
-- Version: V1.0.38
-- Description: Create comprehensive tax management V2 tables for multi-country tax support with Spring multi-tenancy
-- Author: Claude Code
-- Date: 2025-01-09

-- =====================================================
-- Master Tax Codes Table (Global Tax Code Registry)
-- =====================================================
CREATE TABLE master_tax_code (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL,
    code_type VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500) NOT NULL,
    chapter VARCHAR(10),
    heading VARCHAR(10),
    sub_heading VARCHAR(20),
    category VARCHAR(100),
    default_tax_rate DOUBLE PRECISION,
    default_tax_slab_id VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_master_tax_code UNIQUE (country_code, code_type, code)
);

CREATE INDEX idx_master_tax_country ON master_tax_code(country_code);
CREATE INDEX idx_master_tax_code_type ON master_tax_code(code_type);
CREATE INDEX idx_master_tax_code ON master_tax_code(code);
CREATE INDEX idx_master_tax_active ON master_tax_code(is_active);
CREATE INDEX idx_master_tax_updated ON master_tax_code(updated_at);
CREATE INDEX idx_master_tax_lookup ON master_tax_code(country_code, code_type, is_active);

-- =====================================================
-- Tax Configurations Table (Multi-tenant via owner_id)
-- =====================================================
CREATE TABLE tax_configuration (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL,
    tax_strategy VARCHAR(50) NOT NULL,
    default_tax_code_system VARCHAR(50) NOT NULL,
    tax_jurisdictions JSONB,
    industry VARCHAR(100),
    auto_subscribe_new_codes BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_tax_config UNIQUE (owner_id)
);

CREATE INDEX idx_tax_config ON tax_configuration(id);
CREATE INDEX idx_tax_config_country ON tax_configuration(country_code);
CREATE INDEX idx_tax_config_updated ON tax_configuration(updated_at);

-- =====================================================
-- Tax Codes Table (Multi-tenant via owner_id)
-- =====================================================
CREATE TABLE tax_code (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    master_tax_code_id VARCHAR(255) NOT NULL,

    -- Cached master data for offline access
    code VARCHAR(100) NOT NULL,
    code_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500) NOT NULL,

    -- Workspace-specific configuration
    custom_name VARCHAR(255),
    custom_tax_rule_id VARCHAR(255),
    usage_count INT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP(6) NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'SYNCED',

    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_tax_code UNIQUE (owner_id, master_tax_code_id)
);

CREATE INDEX idx_tax_code_workspace ON tax_code(owner_id);
CREATE INDEX idx_tax_code_master ON tax_code(master_tax_code_id);
CREATE INDEX idx_tax_code_updated ON tax_code(updated_at);
CREATE INDEX idx_tax_code_favorite ON tax_code(is_favorite);
CREATE INDEX idx_tax_code_active ON tax_code(is_active);

-- =====================================================
-- Tax Rules Table (Multi-tenant via owner_id)
-- =====================================================
CREATE TABLE tax_rule (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL,

    tax_code_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(100) NOT NULL,
    tax_code_type VARCHAR(50) NOT NULL,
    tax_code_description TEXT,

    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50) NOT NULL,

    component_composition JSONB NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tax_rule ON tax_rule(owner_id);
CREATE INDEX idx_tax_rule_tax_code_id ON tax_rule(tax_code_id);
CREATE INDEX idx_tax_rule_tax_code ON tax_rule(tax_code);
CREATE INDEX idx_tax_rule_country ON tax_rule(country_code);
CREATE INDEX idx_tax_rule_jurisdiction ON tax_rule(jurisdiction);
CREATE INDEX idx_tax_rule_updated ON tax_rule(updated_at);
CREATE INDEX idx_tax_rule_active ON tax_rule(is_active);

-- =====================================================
-- Tax Components Table (Multi-tenant via owner_id)
-- =====================================================
CREATE TABLE tax_component (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    component_type_id VARCHAR(255) NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    component_display_name VARCHAR(200),
    tax_type VARCHAR(50) NOT NULL,

    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50) NOT NULL,

    rate_percentage DOUBLE PRECISION NOT NULL,
    is_compound BOOLEAN NOT NULL DEFAULT FALSE,
    calculation_method VARCHAR(50) NOT NULL DEFAULT 'PERCENTAGE',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tax_comp ON tax_component(owner_id);
CREATE INDEX idx_tax_comp_type ON tax_component(component_type_id);
CREATE INDEX idx_tax_comp_jurisdiction ON tax_component(jurisdiction);
CREATE INDEX idx_tax_comp_updated ON tax_component(updated_at);
CREATE INDEX idx_tax_comp_active ON tax_component(is_active);

-- =====================================================
-- Initial Sample Data for India (IN)
-- =====================================================

-- Insert sample master tax codes for India
INSERT INTO master_tax_code
(uid, country_code, code_type, code, description, short_description, chapter, heading, category, default_tax_rate, default_tax_slab_id, is_active, created_at, updated_at)
VALUES
('MTC_IN_HSN_1001', 'IN', 'HSN_CODE', '1001', 'Live animals; animal products', 'Live animals', '10', '1001', 'AGRICULTURE', 5.0, 'GST_5', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_HSN_8517', 'IN', 'HSN_CODE', '8517', 'Telephone sets, including smartphones and other apparatus for transmission or reception of voice, images or other data', 'Smartphones', '85', '8517', 'ELECTRONICS', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_HSN_3004', 'IN', 'HSN_CODE', '3004', 'Medicaments (excluding goods of heading 30.02, 30.05 or 30.06) consisting of mixed or unmixed products for therapeutic or prophylactic uses', 'Medicines', '30', '3004', 'PHARMACEUTICAL', 12.0, 'GST_12', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_HSN_6109', 'IN', 'HSN_CODE', '6109', 'T-shirts, singlets and other vests, knitted or crocheted', 'T-shirts', '61', '6109', 'TEXTILES', 12.0, 'GST_12', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_HSN_8471', 'IN', 'HSN_CODE', '8471', 'Automatic data processing machines and units thereof; magnetic or optical readers', 'Computers', '84', '8471', 'ELECTRONICS', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_HSN_2710', 'IN', 'HSN_CODE', '2710', 'Petroleum oils and oils obtained from bituminous minerals, other than crude', 'Petroleum products', '27', '2710', 'ENERGY', 28.0, 'GST_28', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_HSN_9403', 'IN', 'HSN_CODE', '9403', 'Other furniture and parts thereof', 'Furniture', '94', '9403', 'FURNITURE', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_SAC_998314', 'IN', 'SAC_CODE', '998314', 'Consulting engineer''s services', 'Engineering services', NULL, NULL, 'SERVICES', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MTC_IN_SAC_996511', 'IN', 'SAC_CODE', '996511', 'Information technology design and development services', 'IT development', NULL, NULL, 'SERVICES', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Master Tax Component Table (Global reference templates)
-- =====================================================
-- NOTE: These are global component templates (no owner_id) that workspaces
-- can reference when creating workspace-specific tax_component records
CREATE TABLE master_tax_component (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    component_type_id VARCHAR(255) NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    component_display_name VARCHAR(200),
    tax_type VARCHAR(50) NOT NULL,
    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50) NOT NULL,
    rate_percentage DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_master_tax_component UNIQUE (component_type_id, rate_percentage)
);

CREATE INDEX idx_master_tax_comp_type ON master_tax_component(component_type_id);
CREATE INDEX idx_master_tax_comp_rate ON master_tax_component(rate_percentage);

-- Insert global tax component templates
INSERT INTO master_tax_component
(uid, component_type_id, component_name, component_display_name, tax_type,
 jurisdiction, jurisdiction_level, rate_percentage, is_active, created_at, updated_at)
VALUES
-- CGST Components (Central GST) - Half of total GST rate
('MCOMP_CGST_0.125', 'TYPE_CGST', 'CGST', 'Central GST 0.125%', 'GST', 'INDIA', 'COUNTRY', 0.125, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_CGST_1.5', 'TYPE_CGST', 'CGST', 'Central GST 1.5%', 'GST', 'INDIA', 'COUNTRY', 1.5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_CGST_2.5', 'TYPE_CGST', 'CGST', 'Central GST 2.5%', 'GST', 'INDIA', 'COUNTRY', 2.5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_CGST_6', 'TYPE_CGST', 'CGST', 'Central GST 6%', 'GST', 'INDIA', 'COUNTRY', 6.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_CGST_9', 'TYPE_CGST', 'CGST', 'Central GST 9%', 'GST', 'INDIA', 'COUNTRY', 9.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_CGST_14', 'TYPE_CGST', 'CGST', 'Central GST 14%', 'GST', 'INDIA', 'COUNTRY', 14.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SGST Components (State GST) - Half of total GST rate
('MCOMP_SGST_0.125', 'TYPE_SGST', 'SGST', 'State GST 0.125%', 'GST', 'INDIA', 'STATE', 0.125, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_SGST_1.5', 'TYPE_SGST', 'SGST', 'State GST 1.5%', 'GST', 'INDIA', 'STATE', 1.5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_SGST_2.5', 'TYPE_SGST', 'SGST', 'State GST 2.5%', 'GST', 'INDIA', 'STATE', 2.5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_SGST_6', 'TYPE_SGST', 'SGST', 'State GST 6%', 'GST', 'INDIA', 'STATE', 6.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_SGST_9', 'TYPE_SGST', 'SGST', 'State GST 9%', 'GST', 'INDIA', 'STATE', 9.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_SGST_14', 'TYPE_SGST', 'SGST', 'State GST 14%', 'GST', 'INDIA', 'STATE', 14.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- IGST Components (Integrated GST) - Full GST rate for inter-state
('MCOMP_IGST_0.25', 'TYPE_IGST', 'IGST', 'Integrated GST 0.25%', 'GST', 'INDIA', 'COUNTRY', 0.25, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_IGST_3', 'TYPE_IGST', 'IGST', 'Integrated GST 3%', 'GST', 'INDIA', 'COUNTRY', 3.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_IGST_5', 'TYPE_IGST', 'IGST', 'Integrated GST 5%', 'GST', 'INDIA', 'COUNTRY', 5.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_IGST_12', 'TYPE_IGST', 'IGST', 'Integrated GST 12%', 'GST', 'INDIA', 'COUNTRY', 12.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_IGST_18', 'TYPE_IGST', 'IGST', 'Integrated GST 18%', 'GST', 'INDIA', 'COUNTRY', 18.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MCOMP_IGST_28', 'TYPE_IGST', 'IGST', 'Integrated GST 28%', 'GST', 'INDIA', 'COUNTRY', 28.0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Workspace Tax Components (Optional system-level examples)
-- =====================================================
-- NOTE: These are optional example workspace-specific components with owner_id = 'SYSTEM'
-- Workspaces typically create their own from master_tax_component templates
INSERT INTO tax_component
(uid, component_type_id, component_name, component_display_name, tax_type,
 jurisdiction, jurisdiction_level, rate_percentage, is_compound, calculation_method,
 is_active, owner_id, created_at, updated_at)
VALUES
-- CGST Components (Central GST) - Half of total GST rate
('COMP_CGST_0.125', 'TYPE_CGST', 'CGST', 'Central GST 0.125%', 'GST', 'INDIA', 'COUNTRY', 0.125, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_CGST_1.5', 'TYPE_CGST', 'CGST', 'Central GST 1.5%', 'GST', 'INDIA', 'COUNTRY', 1.5, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_CGST_2.5', 'TYPE_CGST', 'CGST', 'Central GST 2.5%', 'GST', 'INDIA', 'COUNTRY', 2.5, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_CGST_6', 'TYPE_CGST', 'CGST', 'Central GST 6%', 'GST', 'INDIA', 'COUNTRY', 6.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_CGST_9', 'TYPE_CGST', 'CGST', 'Central GST 9%', 'GST', 'INDIA', 'COUNTRY', 9.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_CGST_14', 'TYPE_CGST', 'CGST', 'Central GST 14%', 'GST', 'INDIA', 'COUNTRY', 14.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SGST Components (State GST) - Half of total GST rate
('COMP_SGST_0.125', 'TYPE_SGST', 'SGST', 'State GST 0.125%', 'GST', 'INDIA', 'STATE', 0.125, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_SGST_1.5', 'TYPE_SGST', 'SGST', 'State GST 1.5%', 'GST', 'INDIA', 'STATE', 1.5, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_SGST_2.5', 'TYPE_SGST', 'SGST', 'State GST 2.5%', 'GST', 'INDIA', 'STATE', 2.5, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_SGST_6', 'TYPE_SGST', 'SGST', 'State GST 6%', 'GST', 'INDIA', 'STATE', 6.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_SGST_9', 'TYPE_SGST', 'SGST', 'State GST 9%', 'GST', 'INDIA', 'STATE', 9.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_SGST_14', 'TYPE_SGST', 'SGST', 'State GST 14%', 'GST', 'INDIA', 'STATE', 14.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- IGST Components (Integrated GST) - Full GST rate for inter-state
('COMP_IGST_0.25', 'TYPE_IGST', 'IGST', 'Integrated GST 0.25%', 'GST', 'INDIA', 'COUNTRY', 0.25, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_IGST_3', 'TYPE_IGST', 'IGST', 'Integrated GST 3%', 'GST', 'INDIA', 'COUNTRY', 3.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_IGST_5', 'TYPE_IGST', 'IGST', 'Integrated GST 5%', 'GST', 'INDIA', 'COUNTRY', 5.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_IGST_12', 'TYPE_IGST', 'IGST', 'Integrated GST 12%', 'GST', 'INDIA', 'COUNTRY', 12.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_IGST_18', 'TYPE_IGST', 'IGST', 'Integrated GST 18%', 'GST', 'INDIA', 'COUNTRY', 18.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COMP_IGST_28', 'TYPE_IGST', 'IGST', 'Integrated GST 28%', 'GST', 'INDIA', 'COUNTRY', 28.0, FALSE, 'PERCENTAGE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Master Tax Rule Table (Global reference templates)
-- =====================================================
-- NOTE: These are global rule templates (no owner_id) that define standard
-- component compositions for each master_tax_code. Workspaces reference these
-- when creating workspace-specific tax_rule records during tax code subscription.
CREATE TABLE master_tax_rule (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL,
    master_tax_code_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(100) NOT NULL,
    tax_code_type VARCHAR(50) NOT NULL,
    tax_rate DOUBLE PRECISION NOT NULL,
    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50) NOT NULL,
    component_composition JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_master_tax_rule UNIQUE (master_tax_code_id, jurisdiction, jurisdiction_level)
);

CREATE INDEX idx_master_tax_rule_code ON master_tax_rule(master_tax_code_id);
CREATE INDEX idx_master_tax_rule_rate ON master_tax_rule(tax_rate);
CREATE INDEX idx_master_tax_rule_country ON master_tax_rule(country_code);

-- Insert global tax rule templates for sample master tax codes
INSERT INTO master_tax_rule
(uid, country_code, master_tax_code_id, tax_code, tax_code_type, tax_rate,
 jurisdiction, jurisdiction_level, component_composition, is_active, created_at, updated_at)
VALUES
-- HSN 1001 - Live Animals (5% GST)
('MTR_IN_1001', 'IN', 'MTC_IN_HSN_1001', '1001', 'HSN_CODE', 5.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 5.0, "components": [{"id": "MCOMP_CGST_2.5", "name": "CGST", "rate": 2.5, "order": 1}, {"id": "MCOMP_SGST_2.5", "name": "SGST", "rate": 2.5, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 5.0, "components": [{"id": "MCOMP_IGST_5", "name": "IGST", "rate": 5.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 8517 - Smartphones (18% GST)
('MTR_IN_8517', 'IN', 'MTC_IN_HSN_8517', '8517', 'HSN_CODE', 18.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "MCOMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 3004 - Medicines (12% GST)
('MTR_IN_3004', 'IN', 'MTC_IN_HSN_3004', '3004', 'HSN_CODE', 12.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 12.0, "components": [{"id": "MCOMP_CGST_6", "name": "CGST", "rate": 6.0, "order": 1}, {"id": "MCOMP_SGST_6", "name": "SGST", "rate": 6.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 12.0, "components": [{"id": "MCOMP_IGST_12", "name": "IGST", "rate": 12.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 6109 - T-shirts (12% GST)
('MTR_IN_6109', 'IN', 'MTC_IN_HSN_6109', '6109', 'HSN_CODE', 12.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 12.0, "components": [{"id": "MCOMP_CGST_6", "name": "CGST", "rate": 6.0, "order": 1}, {"id": "MCOMP_SGST_6", "name": "SGST", "rate": 6.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 12.0, "components": [{"id": "MCOMP_IGST_12", "name": "IGST", "rate": 12.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 8471 - Computers (18% GST)
('MTR_IN_8471', 'IN', 'MTC_IN_HSN_8471', '8471', 'HSN_CODE', 18.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "MCOMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 2710 - Petroleum (28% GST)
('MTR_IN_2710', 'IN', 'MTC_IN_HSN_2710', '2710', 'HSN_CODE', 28.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 28.0, "components": [{"id": "MCOMP_CGST_14", "name": "CGST", "rate": 14.0, "order": 1}, {"id": "MCOMP_SGST_14", "name": "SGST", "rate": 14.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 28.0, "components": [{"id": "MCOMP_IGST_28", "name": "IGST", "rate": 28.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 9403 - Furniture (18% GST)
('MTR_IN_9403', 'IN', 'MTC_IN_HSN_9403', '9403', 'HSN_CODE', 18.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "MCOMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SAC 998314 - Engineering Services (18% GST)
('MTR_IN_998314', 'IN', 'MTC_IN_SAC_998314', '998314', 'SAC_CODE', 18.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "MCOMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SAC 996511 - IT Development Services (18% GST)
('MTR_IN_996511', 'IN', 'MTC_IN_SAC_996511', '996511', 'SAC_CODE', 18.0, 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "MCOMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "MCOMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- Workspace Tax Rules (Optional system-level examples)
-- =====================================================
-- NOTE: These are optional example workspace-specific tax rules with owner_id = 'SYSTEM'
-- Workspaces typically create their own from master_tax_rule templates
INSERT INTO tax_rule
(uid, country_code, tax_code_id, tax_code, tax_code_type, tax_code_description,
 jurisdiction, jurisdiction_level, component_composition, is_active, owner_id, created_at, updated_at)
VALUES
-- HSN 1001 - Live Animals (5% GST)
('TR_SYSTEM_1001', 'IN', 'SYSTEM', '1001', 'HSN_CODE', 'Live animals; animal products',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 5.0, "components": [{"id": "COMP_CGST_2.5", "name": "CGST", "rate": 2.5, "order": 1}, {"id": "COMP_SGST_2.5", "name": "SGST", "rate": 2.5, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 5.0, "components": [{"id": "COMP_IGST_5", "name": "IGST", "rate": 5.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 8517 - Smartphones (18% GST)
('TR_SYSTEM_8517', 'IN', 'SYSTEM', '8517', 'HSN_CODE', 'Telephone sets, including smartphones',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "COMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "COMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "COMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 3004 - Medicines (12% GST)
('TR_SYSTEM_3004', 'IN', 'SYSTEM', '3004', 'HSN_CODE', 'Medicaments',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 12.0, "components": [{"id": "COMP_CGST_6", "name": "CGST", "rate": 6.0, "order": 1}, {"id": "COMP_SGST_6", "name": "SGST", "rate": 6.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 12.0, "components": [{"id": "COMP_IGST_12", "name": "IGST", "rate": 12.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 6109 - T-shirts (12% GST)
('TR_SYSTEM_6109', 'IN', 'SYSTEM', '6109', 'HSN_CODE', 'T-shirts, singlets and other vests',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 12.0, "components": [{"id": "COMP_CGST_6", "name": "CGST", "rate": 6.0, "order": 1}, {"id": "COMP_SGST_6", "name": "SGST", "rate": 6.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 12.0, "components": [{"id": "COMP_IGST_12", "name": "IGST", "rate": 12.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 8471 - Computers (18% GST)
('TR_SYSTEM_8471', 'IN', 'SYSTEM', '8471', 'HSN_CODE', 'Automatic data processing machines',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "COMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "COMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "COMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 2710 - Petroleum (28% GST)
('TR_SYSTEM_2710', 'IN', 'SYSTEM', '2710', 'HSN_CODE', 'Petroleum oils and oils obtained from bituminous minerals',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 28.0, "components": [{"id": "COMP_CGST_14", "name": "CGST", "rate": 14.0, "order": 1}, {"id": "COMP_SGST_14", "name": "SGST", "rate": 14.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 28.0, "components": [{"id": "COMP_IGST_28", "name": "IGST", "rate": 28.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- HSN 9403 - Furniture (18% GST)
('TR_SYSTEM_9403', 'IN', 'SYSTEM', '9403', 'HSN_CODE', 'Other furniture and parts thereof',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "COMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "COMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "COMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SAC 998314 - Engineering Services (18% GST)
('TR_SYSTEM_998314', 'IN', 'SYSTEM', '998314', 'SAC_CODE', 'Consulting engineer''s services',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "COMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "COMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "COMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- SAC 996511 - IT Development Services (18% GST)
('TR_SYSTEM_996511', 'IN', 'SYSTEM', '996511', 'SAC_CODE', 'Information technology design and development services',
 'INDIA', 'COUNTRY',
 '{"INTRA_STATE": {"scenario": "INTRA_STATE", "totalRate": 18.0, "components": [{"id": "COMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1}, {"id": "COMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}]}, "INTER_STATE": {"scenario": "INTER_STATE", "totalRate": 18.0, "components": [{"id": "COMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}]}}'::jsonb,
 TRUE, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- End of Tax Module V2 Database Migration
-- =====================================================
