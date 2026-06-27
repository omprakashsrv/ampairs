-- Analytics Module Database Migration (PostgreSQL)
-- Description: Materialized KPI read model (kpi_daily_summary) + demand forecast (demand_forecast)
-- Feature: 022-analytics-forecasting-dashboard
-- Money: DECIMAL(19,4); quantities DECIMAL(19,3); bucket key business_date DATE (business timezone).

CREATE TABLE kpi_daily_summary (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    business_date DATE NOT NULL,
    metric_group VARCHAR(32) NOT NULL,
    period VARCHAR(8) NOT NULL DEFAULT 'DAY',
    dim_product_id VARCHAR(64) NOT NULL DEFAULT '',
    dim_customer_id VARCHAR(64) NOT NULL DEFAULT '',
    tax_rate DECIMAL(7,4),
    tax_kind VARCHAR(8),
    aging_bucket VARCHAR(16),
    gross_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    net_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    qty DECIMAL(19,3) NOT NULL DEFAULT 0,
    doc_count INT NOT NULL DEFAULT 0,
    recomputed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_kpi_summary_uid ON kpi_daily_summary (uid);
CREATE UNIQUE INDEX ux_kpi_summary_key ON kpi_daily_summary
    (owner_id, business_date, metric_group, dim_product_id, dim_customer_id, tax_rate, tax_kind, aging_bucket);
CREATE INDEX ix_kpi_summary_read ON kpi_daily_summary (owner_id, metric_group, business_date);
CREATE INDEX ix_kpi_summary_dim_product ON kpi_daily_summary (owner_id, metric_group, dim_product_id, business_date);
CREATE INDEX ix_kpi_summary_dim_customer ON kpi_daily_summary (owner_id, metric_group, dim_customer_id, business_date);

COMMENT ON TABLE kpi_daily_summary IS 'Materialized per-business-day KPI summary buckets (recomputable read model)';

CREATE TABLE demand_forecast (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    product_id VARCHAR(64) NOT NULL,
    period_start DATE NOT NULL,
    horizon INT NOT NULL DEFAULT 1,
    mean_qty DECIMAL(19,3) NOT NULL DEFAULT 0,
    std_dev_qty DECIMAL(19,3) NOT NULL DEFAULT 0,
    method VARCHAR(16) NOT NULL DEFAULT 'MOVING_AVG',
    confidence VARCHAR(8) NOT NULL DEFAULT 'LOW',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_forecast_uid ON demand_forecast (uid);
CREATE UNIQUE INDEX ux_forecast_key ON demand_forecast (owner_id, product_id, period_start, horizon);
CREATE INDEX ix_forecast_sync ON demand_forecast (owner_id, updated_at);
CREATE INDEX ix_forecast_product ON demand_forecast (owner_id, product_id, period_start);

COMMENT ON TABLE demand_forecast IS 'Per-product demand forecast (pull-only to clients)';
