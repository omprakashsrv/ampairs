-- DMS Module Migration (MySQL)
-- Version: 1.0.119
-- Description: Brand distribution visibility — secondary-sales / distributor-stock snapshots + targets.

CREATE TABLE secondary_sales_snapshots
(
    id                            BIGINT         NOT NULL AUTO_INCREMENT,
    uid                           VARCHAR(40)    NOT NULL,
    attributed_brand_workspace_id VARCHAR(40)    NOT NULL,
    distributor_workspace_id      VARCHAR(40)    NOT NULL,
    grain                         VARCHAR(20)    NOT NULL DEFAULT 'SKU_PERIOD',
    period_key                    VARCHAR(20)    NOT NULL,
    area_code                     VARCHAR(20),
    brand_product_uid             VARCHAR(40),
    brand_sku_code                VARCHAR(80),
    quantity                      DOUBLE         NOT NULL DEFAULT 0,
    value_amount                  DECIMAL(19, 4) NOT NULL DEFAULT 0,
    version                       INT            NOT NULL DEFAULT 1,
    created_at                    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_sss_uid (uid),
    INDEX idx_sss_brand (attributed_brand_workspace_id),
    INDEX idx_sss_distributor (distributor_workspace_id),
    INDEX idx_sss_period (period_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE distributor_stock_snapshots
(
    id                            BIGINT      NOT NULL AUTO_INCREMENT,
    uid                           VARCHAR(40) NOT NULL,
    attributed_brand_workspace_id VARCHAR(40) NOT NULL,
    distributor_workspace_id      VARCHAR(40) NOT NULL,
    brand_product_uid             VARCHAR(40),
    brand_sku_code                VARCHAR(80),
    on_hand_quantity              DOUBLE      NOT NULL DEFAULT 0,
    as_of                         TIMESTAMP   NOT NULL,
    version                       INT         NOT NULL DEFAULT 1,
    created_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_dss_uid (uid),
    INDEX idx_dss_brand (attributed_brand_workspace_id),
    INDEX idx_dss_distributor (distributor_workspace_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE sales_targets
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    uid                      VARCHAR(40)    NOT NULL,
    tier                     VARCHAR(20)    NOT NULL DEFAULT 'PRIMARY',
    brand_workspace_id       VARCHAR(40)    NOT NULL,
    distributor_workspace_id VARCHAR(40),
    rep_member_uid           VARCHAR(40),
    period_key               VARCHAR(20)    NOT NULL,
    brand_product_uid        VARCHAR(40),
    area_code                VARCHAR(20),
    target_quantity          DOUBLE         NOT NULL DEFAULT 0,
    target_value             DECIMAL(19, 4) NOT NULL DEFAULT 0,
    created_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_target_uid (uid),
    INDEX idx_target_brand (brand_workspace_id),
    INDEX idx_target_distributor (distributor_workspace_id),
    INDEX idx_target_period (period_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
