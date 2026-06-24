-- Workspace Module Database Migration Script
-- Version: 1.0.102
-- Description: Create master_llm_models table (server-seeded on-device LLM model catalog)
-- Dependencies: V1.0.5__create_workspace_module_tables.sql

CREATE TABLE master_llm_models (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    model_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    backend_id VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    download_url VARCHAR(1000) NOT NULL,
    size_bytes BIGINT NOT NULL,
    estimated_peak_memory_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64),
    temperature DOUBLE NOT NULL,
    top_k INT NOT NULL,
    top_p DOUBLE NOT NULL,
    max_tokens INT NOT NULL,
    display_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    CONSTRAINT uk_master_llm_model_uid UNIQUE (uid),
    CONSTRAINT uk_master_llm_model_model_id UNIQUE (model_id)
);

CREATE INDEX idx_master_llm_model_role ON master_llm_models (role);
CREATE INDEX idx_master_llm_model_active ON master_llm_models (active);
