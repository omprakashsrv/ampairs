-- Tax Module V2 Database Migration Script (MySQL)
-- Version: V1.0.38
-- Description: Create comprehensive tax management V2 tables for multi-country tax support
-- Author: Claude Code
-- Date: 2025-12-06

-- =====================================================
-- Master Tax Codes Table (Global Tax Code Registry)
-- =====================================================
CREATE TABLE master_tax_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    default_tax_rate DOUBLE,
    default_tax_slab_id VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSON,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_master_tax_code UNIQUE (country_code, code_type, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_master_tax_country ON master_tax_codes(country_code);
CREATE INDEX idx_master_tax_code_type ON master_tax_codes(code_type);
CREATE INDEX idx_master_tax_code ON master_tax_codes(code);
CREATE INDEX idx_master_tax_active ON master_tax_codes(is_active);
CREATE INDEX idx_master_tax_updated ON master_tax_codes(updated_at);
CREATE INDEX idx_master_tax_lookup ON master_tax_codes(country_code, code_type, is_active);

-- =====================================================
-- Workspace Tax Configurations Table
-- =====================================================
CREATE TABLE workspace_tax_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    tax_strategy VARCHAR(50) NOT NULL,
    default_tax_code_system VARCHAR(50) NOT NULL,
    tax_jurisdictions JSON,
    industry VARCHAR(100),
    auto_subscribe_new_codes BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    metadata JSON,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_workspace_tax_config UNIQUE (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_workspace_tax_config_workspace ON workspace_tax_configurations(workspace_id);
CREATE INDEX idx_workspace_tax_config_country ON workspace_tax_configurations(country_code);
CREATE INDEX idx_workspace_tax_config_updated ON workspace_tax_configurations(updated_at);

-- =====================================================
-- Workspace Tax Codes Table (Subscription Model)
-- =====================================================
CREATE TABLE workspace_tax_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    master_tax_code_id VARCHAR(255) NOT NULL,

    -- Cached master data for offline access
    code VARCHAR(100) NOT NULL,
    code_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500) NOT NULL,

    -- Workspace-specific configuration
    custom_tax_rule_id VARCHAR(255),
    usage_count INT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP(6) NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sync_status VARCHAR(50) NOT NULL DEFAULT 'SYNCED',

    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_workspace_tax_code UNIQUE (workspace_id, master_tax_code_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_workspace_tax_code_workspace ON workspace_tax_codes(workspace_id);
CREATE INDEX idx_workspace_tax_code_master ON workspace_tax_codes(master_tax_code_id);
CREATE INDEX idx_workspace_tax_code_updated ON workspace_tax_codes(updated_at);
CREATE INDEX idx_workspace_tax_code_favorite ON workspace_tax_codes(is_favorite);
CREATE INDEX idx_workspace_tax_code_active ON workspace_tax_codes(is_active);

-- =====================================================
-- Tax Rules V2 Table
-- =====================================================
CREATE TABLE tax_rules_v2 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,

    workspace_tax_code_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(100) NOT NULL,
    tax_code_type VARCHAR(50) NOT NULL,
    tax_code_description TEXT,

    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50) NOT NULL,

    component_composition JSON NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tax_rule_v2_workspace ON tax_rules_v2(workspace_id);
CREATE INDEX idx_tax_rule_v2_workspace_tax_code ON tax_rules_v2(workspace_tax_code_id);
CREATE INDEX idx_tax_rule_v2_tax_code ON tax_rules_v2(tax_code);
CREATE INDEX idx_tax_rule_v2_country ON tax_rules_v2(country_code);
CREATE INDEX idx_tax_rule_v2_jurisdiction ON tax_rules_v2(jurisdiction);
CREATE INDEX idx_tax_rule_v2_updated ON tax_rules_v2(updated_at);
CREATE INDEX idx_tax_rule_v2_active ON tax_rules_v2(is_active);

-- =====================================================
-- Workspace Tax Components Table
-- =====================================================
CREATE TABLE workspace_tax_components (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    component_type_id VARCHAR(255) NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    component_display_name VARCHAR(200),
    tax_type VARCHAR(50) NOT NULL,

    jurisdiction VARCHAR(100) NOT NULL,
    jurisdiction_level VARCHAR(50) NOT NULL,

    rate_percentage DOUBLE NOT NULL,
    is_compound BOOLEAN NOT NULL DEFAULT FALSE,
    calculation_method VARCHAR(50) NOT NULL DEFAULT 'PERCENTAGE',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_workspace_tax_comp_workspace ON workspace_tax_components(workspace_id);
CREATE INDEX idx_workspace_tax_comp_type ON workspace_tax_components(component_type_id);
CREATE INDEX idx_workspace_tax_comp_jurisdiction ON workspace_tax_components(jurisdiction);
CREATE INDEX idx_workspace_tax_comp_updated ON workspace_tax_components(updated_at);
CREATE INDEX idx_workspace_tax_comp_active ON workspace_tax_components(is_active);

-- =====================================================
-- Initial Sample Data for India (IN)
-- =====================================================

-- Insert sample master tax codes for India
INSERT INTO master_tax_codes
(uid, country_code, code_type, code, description, short_description, chapter, heading, category, default_tax_rate, default_tax_slab_id, is_active, created_at, updated_at)
VALUES
('MTC_IN_HSN_1001', 'IN', 'HSN_CODE', '1001', 'Wheat and meslin', 'Wheat', '10', '1001', 'AGRICULTURE', 0.0, 'GST_0', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_HSN_1006', 'IN', 'HSN_CODE', '1006', 'Rice', 'Rice', '10', '1006', 'AGRICULTURE', 0.0, 'GST_0', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_HSN_1701', 'IN', 'HSN_CODE', '1701', 'Cane or beet sugar and chemically pure sucrose', 'Sugar', '17', '1701', 'FOOD', 5.0, 'GST_5', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_HSN_1905', 'IN', 'HSN_CODE', '1905', 'Bread, pastry, cakes, biscuits', 'Biscuits', '19', '1905', 'FOOD', 5.0, 'GST_5', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_HSN_2201', 'IN', 'HSN_CODE', '2201', 'Waters, including natural or artificial mineral waters', 'Water', '22', '2201', 'BEVERAGES', 9.0, 'GST_9', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_HSN_6109', 'IN', 'HSN_CODE', '6109', 'T-shirts, singlets and other vests, knitted or crocheted', 'T-shirts', '61', '6109', 'TEXTILES', 5.0, 'GST_5', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_HSN_8517', 'IN', 'HSN_CODE', '8517', 'Telephone sets, including smartphones', 'Smartphones', '85', '8517', 'ELECTRONICS', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_SAC_998314', 'IN', 'SAC_CODE', '998314', 'Information technology design and development services', 'IT Services', NULL, NULL, 'SERVICES', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('MTC_IN_SAC_996511', 'IN', 'SAC_CODE', '996511', 'Accounting, auditing and book-keeping services', 'Accounting', NULL, NULL, 'SERVICES', 18.0, 'GST_18', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- =====================================================
-- End of Tax Module V2 Database Migration
-- =====================================================
