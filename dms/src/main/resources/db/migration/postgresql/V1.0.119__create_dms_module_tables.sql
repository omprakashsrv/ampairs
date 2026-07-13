-- DMS Module Migration (PostgreSQL)
-- Version: 1.0.119
-- Description: Brand distribution visibility — secondary-sales / distributor-stock snapshots + targets.
--              Snapshots are versioned, recomputable; carry the as-of-sale brand attribution.

CREATE TABLE secondary_sales_snapshots
(
    id                            BIGSERIAL PRIMARY KEY,
    uid                           VARCHAR(40)    NOT NULL UNIQUE,
    attributed_brand_workspace_id VARCHAR(40)    NOT NULL,
    distributor_workspace_id      VARCHAR(40)    NOT NULL,
    grain                         VARCHAR(20)    NOT NULL DEFAULT 'SKU_PERIOD',
    period_key                    VARCHAR(20)    NOT NULL,
    area_code                     VARCHAR(20),
    brand_product_uid             VARCHAR(40),
    brand_sku_code                VARCHAR(80),
    quantity                      DOUBLE PRECISION NOT NULL DEFAULT 0,
    value_amount                  DECIMAL(19, 4) NOT NULL DEFAULT 0,
    version                       INT            NOT NULL DEFAULT 1,
    created_at                    TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sss_brand ON secondary_sales_snapshots (attributed_brand_workspace_id);
CREATE INDEX idx_sss_distributor ON secondary_sales_snapshots (distributor_workspace_id);
CREATE INDEX idx_sss_period ON secondary_sales_snapshots (period_key);

CREATE TABLE distributor_stock_snapshots
(
    id                            BIGSERIAL PRIMARY KEY,
    uid                           VARCHAR(40)      NOT NULL UNIQUE,
    attributed_brand_workspace_id VARCHAR(40)      NOT NULL,
    distributor_workspace_id      VARCHAR(40)      NOT NULL,
    brand_product_uid             VARCHAR(40),
    brand_sku_code                VARCHAR(80),
    on_hand_quantity              DOUBLE PRECISION NOT NULL DEFAULT 0,
    as_of                         TIMESTAMP(6)     NOT NULL,
    version                       INT              NOT NULL DEFAULT 1,
    created_at                    TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_dss_brand ON distributor_stock_snapshots (attributed_brand_workspace_id);
CREATE INDEX idx_dss_distributor ON distributor_stock_snapshots (distributor_workspace_id);

CREATE TABLE sales_targets
(
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(40)      NOT NULL UNIQUE,
    tier                     VARCHAR(20)      NOT NULL DEFAULT 'PRIMARY',
    brand_workspace_id       VARCHAR(40)      NOT NULL,
    distributor_workspace_id VARCHAR(40),
    rep_member_uid           VARCHAR(40),
    period_key               VARCHAR(20)      NOT NULL,
    brand_product_uid        VARCHAR(40),
    area_code                VARCHAR(20),
    target_quantity          DOUBLE PRECISION NOT NULL DEFAULT 0,
    target_value             DECIMAL(19, 4)   NOT NULL DEFAULT 0,
    created_at               TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_target_brand ON sales_targets (brand_workspace_id);
CREATE INDEX idx_target_distributor ON sales_targets (distributor_workspace_id);
CREATE INDEX idx_target_period ON sales_targets (period_key);
