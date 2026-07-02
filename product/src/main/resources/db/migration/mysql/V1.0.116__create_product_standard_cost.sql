-- Effective-dated standard purchase cost (MySQL)
-- Cost-side mirror of price_list_item effective dating (Tally "Standard Cost" / "Applicable From").
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE product_standard_cost (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    product_id VARCHAR(200) NOT NULL,
    variant_sku VARCHAR(200),
    cost_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    effective_from TIMESTAMP NULL DEFAULT NULL,
    effective_to TIMESTAMP NULL DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_product_standard_cost_owner (owner_id),
    INDEX idx_product_standard_cost_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
