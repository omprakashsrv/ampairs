-- Workspace Module Database Migration Script (PostgreSQL)
-- Version: 1.0.102
-- Description: Create master_llm_models table (server-seeded on-device LLM model catalog)
-- Dependencies: V1.0.5__create_workspace_module_tables.sql

CREATE TABLE master_llm_models (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    model_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    backend_id VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    download_url VARCHAR(1000) NOT NULL,
    size_bytes BIGINT NOT NULL,
    estimated_peak_memory_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64),
    temperature DOUBLE PRECISION NOT NULL,
    top_k INT NOT NULL,
    top_p DOUBLE PRECISION NOT NULL,
    max_tokens INT NOT NULL,
    display_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_master_llm_model_role ON master_llm_models (role);
CREATE INDEX idx_master_llm_model_active ON master_llm_models (active);
