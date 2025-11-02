-- Tax Module Database Migration Script
-- Version: 3.1
-- Description: Create comprehensive tax management tables for Indian GST compliance
-- Author: Claude Code
-- Date: 2025-01-20

-- =====================================================
-- Business Types Table
-- =====================================================
CREATE TABLE business_types
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    uid                     VARCHAR(200) NOT NULL UNIQUE,
    business_type           VARCHAR(30)  NOT NULL UNIQUE,
    display_name            VARCHAR(100) NOT NULL,
    description             TEXT,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    default_gst_rate        DECIMAL(8, 4),
    composition_scheme_rate DECIMAL(8, 4),
    turnover_threshold      DECIMAL(15, 2),
    special_rules           JSON,
    compliance_requirements JSON,
    owner_id                VARCHAR(200) NOT NULL,
    ref_id                  VARCHAR(255),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    soft_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    PRIMARY KEY (id),
    INDEX                   idx_business_type_code (business_type),
    INDEX                   idx_business_type_active (is_active),
    INDEX                   idx_business_type_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- HSN Codes Table
-- =====================================================
CREATE TABLE hsn_codes
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    uid                     VARCHAR(200) NOT NULL UNIQUE,
    hsn_code                VARCHAR(10)  NOT NULL UNIQUE,
    hsn_description         TEXT         NOT NULL,
    hsn_chapter             VARCHAR(2),
    hsn_heading             VARCHAR(4),
    parent_hsn_id           BIGINT,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    level                   INT          NOT NULL DEFAULT 1,
    unit_of_measurement     VARCHAR(50),
    exemption_available     BOOLEAN      NOT NULL DEFAULT FALSE,
    business_category_rules JSON,
    attributes              JSON,
    effective_from          TIMESTAMP,
    effective_to            TIMESTAMP,
    owner_id                VARCHAR(200) NOT NULL,
    ref_id                  VARCHAR(255),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    soft_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    PRIMARY KEY (id),
    INDEX                   idx_hsn_code (hsn_code),
    INDEX                   idx_hsn_chapter (hsn_chapter),
    INDEX                   idx_hsn_heading (hsn_heading),
    INDEX                   idx_hsn_parent (parent_hsn_id),
    INDEX                   idx_hsn_active (is_active),
    INDEX                   idx_hsn_level (level),
    INDEX                   idx_hsn_uid (uid),

    FOREIGN KEY (parent_hsn_id) REFERENCES hsn_codes (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Tax Rates Table
-- =====================================================
CREATE TABLE tax_rates
(
    id                               BIGINT        NOT NULL AUTO_INCREMENT,
    uid                              VARCHAR(200)  NOT NULL UNIQUE,
    hsn_code_id                      BIGINT        NOT NULL,
    tax_component_type               VARCHAR(30)   NOT NULL,
    rate_percentage                  DECIMAL(8, 4) NOT NULL DEFAULT 0.0000,
    fixed_amount_per_unit            DECIMAL(12, 4),
    minimum_amount                   DECIMAL(12, 4),
    maximum_amount                   DECIMAL(12, 4),
    business_type                    VARCHAR(30)   NOT NULL,
    geographical_zone                VARCHAR(30),
    effective_from                   DATE          NOT NULL,
    effective_to                     DATE,
    is_active                        BOOLEAN       NOT NULL DEFAULT TRUE,
    version_number                   INT           NOT NULL DEFAULT 1,
    notification_number              VARCHAR(100),
    notification_date                DATE,
    conditions                       JSON,
    exemption_rules                  JSON,
    is_reverse_charge_applicable     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_composition_scheme_applicable BOOLEAN       NOT NULL DEFAULT TRUE,
    description                      TEXT,
    source_reference                 VARCHAR(255),
    owner_id                         VARCHAR(200)  NOT NULL,
    ref_id                           VARCHAR(255),
    active                           BOOLEAN       NOT NULL DEFAULT TRUE,
    soft_deleted                     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at                       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    PRIMARY KEY (id),
    INDEX                            idx_tax_rate_hsn (hsn_code_id),
    INDEX                            idx_tax_rate_component (tax_component_type),
    INDEX                            idx_tax_rate_business (business_type),
    INDEX                            idx_tax_rate_zone (geographical_zone),
    INDEX                            idx_tax_rate_effective (effective_from, effective_to),
    INDEX                            idx_tax_rate_active (is_active),
    INDEX                            idx_tax_rate_lookup (hsn_code_id, business_type, effective_from, is_active),
    INDEX                            idx_tax_rate_uid (uid),

    UNIQUE INDEX uk_tax_rates (hsn_code_id, tax_component_type, business_type, geographical_zone, effective_from),

    FOREIGN KEY (hsn_code_id) REFERENCES hsn_codes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Tax Configurations Table
-- =====================================================
CREATE TABLE tax_configurations
(
    id                               BIGINT        NOT NULL AUTO_INCREMENT,
    uid                              VARCHAR(200)  NOT NULL UNIQUE,
    business_type_id                 BIGINT        NOT NULL,
    hsn_code_id                      BIGINT        NOT NULL,
    geographical_zone                VARCHAR(30),
    total_gst_rate                   DECIMAL(8, 4) NOT NULL DEFAULT 0.0000,
    cgst_rate                        DECIMAL(8, 4),
    sgst_rate                        DECIMAL(8, 4),
    igst_rate                        DECIMAL(8, 4),
    utgst_rate                       DECIMAL(8, 4),
    cess_rate                        DECIMAL(8, 4),
    cess_amount_per_unit             DECIMAL(12, 4),
    effective_from                   DATE          NOT NULL,
    effective_to                     DATE,
    is_active                        BOOLEAN       NOT NULL DEFAULT TRUE,
    is_reverse_charge_applicable     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_composition_scheme_applicable BOOLEAN       NOT NULL DEFAULT TRUE,
    composition_rate                 DECIMAL(8, 4),
    special_conditions               JSON,
    exemption_criteria               JSON,
    threshold_limits                 JSON,
    description                      TEXT,
    notification_reference           VARCHAR(255),
    last_updated_by                  VARCHAR(200),
    owner_id                         VARCHAR(200)  NOT NULL,
    ref_id                           VARCHAR(255),
    active                           BOOLEAN       NOT NULL DEFAULT TRUE,
    soft_deleted                     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at                       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    PRIMARY KEY (id),
    INDEX                            idx_tax_config_business (business_type_id),
    INDEX                            idx_tax_config_hsn (hsn_code_id),
    INDEX                            idx_tax_config_zone (geographical_zone),
    INDEX                            idx_tax_config_effective (effective_from, effective_to),
    INDEX                            idx_tax_config_active (is_active),
    INDEX                            idx_tax_config_lookup (business_type_id, hsn_code_id, effective_from, is_active),
    INDEX                            idx_tax_config_uid (uid),

    UNIQUE INDEX uk_tax_configurations (business_type_id, hsn_code_id, geographical_zone, effective_from),

    FOREIGN KEY (business_type_id) REFERENCES business_types (id) ON DELETE CASCADE,
    FOREIGN KEY (hsn_code_id) REFERENCES hsn_codes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Initial Data Population
-- =====================================================

-- Insert standard business types
INSERT INTO business_types (uid, business_type, display_name, description, default_gst_rate, composition_scheme_rate,
                            turnover_threshold, is_active, owner_id, active, soft_deleted)
VALUES ('BT_B2B_001', 'B2B', 'Business to Business', 'Regular business transactions between GST registered entities',
        18.0000, NULL, NULL, TRUE, 'SYSTEM', TRUE, FALSE),
       ('BT_B2C_001', 'B2C', 'Business to Consumer', 'Retail transactions with end consumers', 18.0000, NULL, NULL,
        TRUE, 'SYSTEM', TRUE, FALSE),
       ('BT_COMP_001', 'COMPOSITION', 'Composition Scheme', 'Simplified GST scheme for small businesses', 1.0000,
        1.0000, 15000000.00, TRUE, 'SYSTEM', TRUE, FALSE),
       ('BT_EXP_001', 'EXPORT', 'Export', 'Goods exported outside India', 0.0000, NULL, NULL, TRUE, 'SYSTEM', TRUE,
        FALSE),
       ('BT_KIRA_001', 'KIRANA', 'Kirana Store', 'Traditional grocery and convenience stores', 5.0000, 0.5000,
        4000000.00, TRUE, 'SYSTEM', TRUE, FALSE),
       ('BT_JEWL_001', 'JEWELRY', 'Jewelry Business', 'Gold, silver and precious metal trading', 3.0000, 1.0000,
        15000000.00, TRUE, 'SYSTEM', TRUE, FALSE),
       ('BT_HARD_001', 'HARDWARE', 'Hardware Store', 'Construction and electrical materials', 18.0000, 1.0000,
        15000000.00, TRUE, 'SYSTEM', TRUE, FALSE),
       ('BT_TOBC_001', 'TOBACCO', 'Tobacco Products', 'Cigarettes, bidis and tobacco products', 40.0000, NULL, NULL,
        TRUE, 'SYSTEM', TRUE, FALSE);

-- Insert common HSN codes with 2025 GST rates
INSERT INTO hsn_codes (uid, hsn_code, hsn_description, hsn_chapter, hsn_heading, level, exemption_available, is_active,
                       owner_id, active, soft_deleted)
VALUES
-- Food items (Chapter 10-21)
('HSN_1001_001', '1001', 'Wheat and meslin', '10', '1001', 2, TRUE, TRUE, 'SYSTEM', TRUE, FALSE),
('HSN_1006_001', '1006', 'Rice', '10', '1006', 2, TRUE, TRUE, 'SYSTEM', TRUE, FALSE),
('HSN_1701_001', '1701', 'Cane or beet sugar and chemically pure sucrose', '17', '1701', 2, FALSE, TRUE, 'SYSTEM', TRUE,
 FALSE),
('HSN_1905_001', '1905', 'Bread, pastry, cakes, biscuits', '19', '1905', 2, FALSE, TRUE, 'SYSTEM', TRUE, FALSE),
('HSN_2201_001', '2201', 'Waters, including natural or artificial mineral waters', '22', '2201', 2, FALSE, TRUE,
 'SYSTEM', TRUE, FALSE),
('HSN_2202_001', '2202', 'Waters containing added sugar or other sweetening matter', '22', '2202', 2, FALSE, TRUE,
 'SYSTEM', TRUE, FALSE),
-- Tobacco products (Chapter 24)
('HSN_2402_001', '2402', 'Cigars, cheroots, cigarillos and cigarettes', '24', '2402', 2, FALSE, TRUE, 'SYSTEM', TRUE,
 FALSE),
('HSN_2403_001', '2403', 'Other manufactured tobacco and manufactured tobacco substitutes', '24', '2403', 2, FALSE,
 TRUE, 'SYSTEM', TRUE, FALSE),
-- Textiles (Chapter 50-63)
('HSN_6109_001', '6109', 'T-shirts, singlets and other vests, knitted or crocheted', '61', '6109', 2, FALSE, TRUE,
 'SYSTEM', TRUE, FALSE),
('HSN_6203_001', '6203', 'Mens or boys suits, ensembles, jackets, blazers, trousers', '62', '6203', 2, FALSE, TRUE,
 'SYSTEM', TRUE, FALSE),
-- Electronics (Chapter 85)
('HSN_8517_001', '8517', 'Telephone sets, including smartphones', '85', '8517', 2, FALSE, TRUE, 'SYSTEM', TRUE, FALSE),
('HSN_8528_001', '8528', 'Monitors and projectors, television receivers', '85', '8528', 2, FALSE, TRUE, 'SYSTEM', TRUE,
 FALSE),
-- Vehicles (Chapter 87)
('HSN_8703_001', '8703', 'Motor cars and other motor vehicles for transport of persons', '87', '8703', 2, FALSE, TRUE,
 'SYSTEM', TRUE, FALSE),
-- Precious metals (Chapter 71)
('HSN_7108_001', '7108', 'Gold, including platinum-plated gold', '71', '7108', 2, FALSE, TRUE, 'SYSTEM', TRUE, FALSE),
('HSN_7113_001', '7113', 'Articles of jewellery and parts thereof, of precious metal', '71', '7113', 2, FALSE, TRUE,
 'SYSTEM', TRUE, FALSE);

-- Sample tax configurations for common business scenarios (2025 rates)
INSERT INTO tax_configurations (uid, business_type_id, hsn_code_id, total_gst_rate, cgst_rate, sgst_rate, igst_rate,
                                effective_from, is_active, description, owner_id, active, soft_deleted)
VALUES
-- Essential food items - 0% GST
('TC_B2B_1001_001', 1, 1, 0.0000, 0.0000, 0.0000, 0.0000, '2025-09-01', TRUE, 'Wheat - Essential food item, 0% GST',
 'SYSTEM', TRUE, FALSE),
('TC_B2B_1006_001', 1, 2, 0.0000, 0.0000, 0.0000, 0.0000, '2025-09-01', TRUE, 'Rice - Essential food item, 0% GST',
 'SYSTEM', TRUE, FALSE),

-- Processed foods - 5% GST
('TC_B2B_1905_001', 1, 4, 5.0000, 2.5000, 2.5000, 5.0000, '2025-09-01', TRUE,
 'Biscuits and cakes - 5% GST (reduced from 18%)', 'SYSTEM', TRUE, FALSE),

-- Beverages - 9% GST (regular) / 40% GST (aerated)
('TC_B2B_2201_001', 1, 5, 9.0000, 4.5000, 4.5000, 9.0000, '2025-09-01', TRUE,
 'Water and non-aerated beverages - 9% GST', 'SYSTEM', TRUE, FALSE),
('TC_B2B_2202_001', 1, 6, 40.0000, 20.0000, 20.0000, 40.0000, '2025-09-01', TRUE,
 'Aerated beverages - 40% GST (luxury slab)', 'SYSTEM', TRUE, FALSE),

-- Tobacco products - 40% GST
('TC_B2B_2402_001', 1, 7, 40.0000, 20.0000, 20.0000, 40.0000, '2025-09-01', TRUE, 'Cigarettes - 40% GST plus cess',
 'SYSTEM', TRUE, FALSE),
('TC_B2B_2403_001', 1, 8, 40.0000, 20.0000, 20.0000, 40.0000, '2025-09-01', TRUE, 'Other tobacco - 40% GST plus cess',
 'SYSTEM', TRUE, FALSE),

-- Textiles - 5% GST (reduced from 18%)
('TC_B2B_6109_001', 1, 9, 5.0000, 2.5000, 2.5000, 5.0000, '2025-09-01', TRUE, 'T-shirts - 5% GST (personal care item)',
 'SYSTEM', TRUE, FALSE),
('TC_B2B_6203_001', 1, 10, 5.0000, 2.5000, 2.5000, 5.0000, '2025-09-01', TRUE, 'Mens garments - 5% GST', 'SYSTEM', TRUE,
 FALSE),

-- Electronics - 18% GST
('TC_B2B_8517_001', 1, 11, 18.0000, 9.0000, 9.0000, 18.0000, '2025-09-01', TRUE, 'Smartphones - 18% GST', 'SYSTEM',
 TRUE, FALSE),
('TC_B2B_8528_001', 1, 12, 18.0000, 9.0000, 9.0000, 18.0000, '2025-09-01', TRUE, 'Television sets - 18% GST', 'SYSTEM',
 TRUE, FALSE),

-- Vehicles - 18% GST
('TC_B2B_8703_001', 1, 13, 18.0000, 9.0000, 9.0000, 18.0000, '2025-09-01', TRUE, 'Motor cars - 18% GST', 'SYSTEM', TRUE,
 FALSE),

-- Precious metals - 3% GST
('TC_B2B_7108_001', 1, 14, 3.0000, 1.5000, 1.5000, 3.0000, '2025-09-01', TRUE, 'Gold - 3% GST on metal value', 'SYSTEM',
 TRUE, FALSE),
('TC_B2B_7113_001', 1, 15, 5.0000, 2.5000, 2.5000, 5.0000, '2025-09-01', TRUE,
 'Gold jewellery - 5% GST on making charges', 'SYSTEM', TRUE, FALSE);

-- Create corresponding tax rates for each component
INSERT INTO tax_rates (uid, hsn_code_id, tax_component_type, rate_percentage, business_type,
                       effective_from, is_active, description, owner_id, active, soft_deleted)
VALUES
-- Processed foods - CGST/SGST/IGST
('TR_1905_CGST_001', 4, 'CGST', 2.5000, 'B2B', '2025-09-01', TRUE, 'CGST for biscuits and cakes', 'SYSTEM', TRUE,
 FALSE),
('TR_1905_SGST_001', 4, 'SGST', 2.5000, 'B2B', '2025-09-01', TRUE, 'SGST for biscuits and cakes', 'SYSTEM', TRUE,
 FALSE),
('TR_1905_IGST_001', 4, 'IGST', 5.0000, 'B2B', '2025-09-01', TRUE, 'IGST for biscuits and cakes', 'SYSTEM', TRUE,
 FALSE),

-- Aerated beverages - 40% GST
('TR_2202_CGST_001', 6, 'CGST', 20.0000, 'B2B', '2025-09-01', TRUE, 'CGST for aerated beverages', 'SYSTEM', TRUE,
 FALSE),
('TR_2202_SGST_001', 6, 'SGST', 20.0000, 'B2B', '2025-09-01', TRUE, 'SGST for aerated beverages', 'SYSTEM', TRUE,
 FALSE),
('TR_2202_IGST_001', 6, 'IGST', 40.0000, 'B2B', '2025-09-01', TRUE, 'IGST for aerated beverages', 'SYSTEM', TRUE,
 FALSE),

-- Electronics - 18% GST
('TR_8517_CGST_001', 11, 'CGST', 9.0000, 'B2B', '2025-09-01', TRUE, 'CGST for smartphones', 'SYSTEM', TRUE, FALSE),
('TR_8517_SGST_001', 11, 'SGST', 9.0000, 'B2B', '2025-09-01', TRUE, 'SGST for smartphones', 'SYSTEM', TRUE, FALSE),
('TR_8517_IGST_001', 11, 'IGST', 18.0000, 'B2B', '2025-09-01', TRUE, 'IGST for smartphones', 'SYSTEM', TRUE, FALSE),

-- Precious metals - 3% GST
('TR_7108_CGST_001', 14, 'CGST', 1.5000, 'B2B', '2025-09-01', TRUE, 'CGST for gold', 'SYSTEM', TRUE, FALSE),
('TR_7108_SGST_001', 14, 'SGST', 1.5000, 'B2B', '2025-09-01', TRUE, 'SGST for gold', 'SYSTEM', TRUE, FALSE),
('TR_7108_IGST_001', 14, 'IGST', 3.0000, 'B2B', '2025-09-01', TRUE, 'IGST for gold', 'SYSTEM', TRUE, FALSE);

-- Create triggers for automatic timestamp updates
DELIMITER
$$

CREATE TRIGGER business_types_updated_at
    BEFORE UPDATE
    ON business_types
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END$$

    CREATE TRIGGER hsn_codes_updated_at
        BEFORE UPDATE
        ON hsn_codes
        FOR EACH ROW
    BEGIN
        SET NEW.updated_at = CURRENT_TIMESTAMP;
END$$

        CREATE TRIGGER tax_rates_updated_at
            BEFORE UPDATE
            ON tax_rates
            FOR EACH ROW
        BEGIN
            SET NEW.updated_at = CURRENT_TIMESTAMP;
END$$

            CREATE TRIGGER tax_configurations_updated_at
                BEFORE UPDATE
                ON tax_configurations
                FOR EACH ROW
            BEGIN
                SET NEW.updated_at = CURRENT_TIMESTAMP;
END$$

                DELIMITER ;

-- =====================================================
-- End of Tax Module Database Migration
-- =====================================================