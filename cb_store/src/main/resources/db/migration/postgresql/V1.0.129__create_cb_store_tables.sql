-- cb_store Module Migration (PostgreSQL)
-- Version: 1.0.129
-- Description: California Burrito outlets + zonal offices
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE zonal_office (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cb_zonal_office_owner ON zonal_office(owner_id);

COMMENT ON TABLE zonal_office IS 'California Burrito city-level maintenance offices';

CREATE TABLE store (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    zonal_office_id VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_cb_store_owner_code UNIQUE (owner_id, code)
);

CREATE INDEX idx_cb_store_owner ON store(owner_id);
CREATE INDEX idx_cb_store_zone ON store(zonal_office_id);

COMMENT ON TABLE store IS 'California Burrito outlets, zone-routed for maintenance';
