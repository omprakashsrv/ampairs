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
-- Sample Tax Rules (Multi-tenant - requires owner_id)
-- =====================================================
-- NOTE: Tax rules are workspace-specific and created when a workspace
-- subscribes to a tax code. Below are commented examples showing the
-- expected component_composition JSON format for Indian GST.
--
-- For 18% GST rate breakdown:
-- INTRA_STATE (within same state):
--   - CGST (Central GST): 9% (half of total)
--   - SGST (State GST): 9% (half of total)
-- INTER_STATE (between different states):
--   - IGST (Integrated GST): 18% (full rate)
--
-- Example tax rule for HSN 8517 (Smartphones - 18% GST):
/*
INSERT INTO tax_rule
(uid, country_code, tax_code_id, tax_code, tax_code_type, tax_code_description,
 jurisdiction, jurisdiction_level, component_composition, is_active, owner_id, created_at, updated_at)
VALUES
('TR_IN_8517_001', 'IN', 'WTC_<workspace_tax_code_id>', '8517', 'HSN_CODE', 'Smartphones',
 'INDIA', 'COUNTRY',
 '{
   "INTRA_STATE": {
     "scenario": "INTRA_STATE",
     "totalRate": 18.0,
     "components": [
       {"id": "COMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1},
       {"id": "COMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}
     ]
   },
   "INTER_STATE": {
     "scenario": "INTER_STATE",
     "totalRate": 18.0,
     "components": [
       {"id": "COMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}
     ]
   }
 }'::jsonb,
 TRUE, '<workspace_owner_id>', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- For 5% GST (e.g., HSN 1001 - Agriculture):
-- INTRA_STATE: CGST 2.5% + SGST 2.5%
-- INTER_STATE: IGST 5%

-- For 12% GST (e.g., HSN 3004 - Medicines):
-- INTRA_STATE: CGST 6% + SGST 6%
-- INTER_STATE: IGST 12%

-- For 28% GST (e.g., HSN 2710 - Petroleum):
-- INTRA_STATE: CGST 14% + SGST 14%
-- INTER_STATE: IGST 28%
*/

-- =====================================================
-- End of Tax Module V2 Database Migration
-- =====================================================
