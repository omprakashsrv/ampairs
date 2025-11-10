-- Create app_versions table for managing desktop app updates (macOS, Windows, Linux)
CREATE TABLE app_versions (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,

    -- Version information
    version VARCHAR(50) NOT NULL,
    version_code INT NOT NULL,
    platform VARCHAR(20) NOT NULL,

    -- Update metadata
    is_mandatory BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,

    -- File information
    download_url TEXT NOT NULL,
    file_size_mb DECIMAL(10, 2),
    file_path VARCHAR(500),
    checksum VARCHAR(128),

    -- Release information
    release_date TIMESTAMP,
    release_notes TEXT,
    min_supported_version VARCHAR(50),

    -- Audit fields (using Instant for UTC timestamps as per CLAUDE.md)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT uk_app_versions_version_platform UNIQUE(version, platform),
    CONSTRAINT chk_app_versions_platform CHECK (platform IN ('MACOS', 'WINDOWS', 'LINUX'))
);

-- Index for fast lookup of latest version per platform
CREATE INDEX idx_app_versions_platform_active
    ON app_versions(platform, is_active, version_code DESC);

-- Index for active versions
CREATE INDEX idx_app_versions_active
    ON app_versions(is_active, release_date DESC);

-- Table comment
COMMENT ON TABLE app_versions IS 'Stores desktop app version information for in-app updates';
