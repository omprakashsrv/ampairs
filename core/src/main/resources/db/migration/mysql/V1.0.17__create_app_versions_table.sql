-- Create app_versions table for managing desktop app updates (macOS, Windows, Linux)
CREATE TABLE app_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,       -- BaseDomain required field

    -- Version information
    version VARCHAR(50) NOT NULL,           -- e.g., "1.0.0.10"
    version_code INT NOT NULL,              -- e.g., 10
    platform VARCHAR(20) NOT NULL,          -- "MACOS", "WINDOWS", "LINUX"

    -- Update metadata
    is_mandatory BOOLEAN DEFAULT FALSE,     -- Force user to update
    is_active BOOLEAN DEFAULT TRUE,         -- Enable/disable this version

    -- File information
    download_url TEXT NOT NULL,             -- Full URL to binary file
    file_size_mb DECIMAL(10, 2),           -- File size in megabytes
    file_path VARCHAR(500),                 -- S3 key or server file path
    checksum VARCHAR(128),                  -- SHA-256 hash for verification

    -- Release information
    release_date TIMESTAMP NULL,
    release_notes TEXT,
    min_supported_version VARCHAR(50),      -- Minimum version that can upgrade

    -- Audit fields (using Instant for UTC timestamps as per CLAUDE.md)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT uk_app_versions_version_platform UNIQUE(version, platform),
    CONSTRAINT chk_app_versions_platform CHECK (platform IN ('MACOS', 'WINDOWS', 'LINUX'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for fast lookup of latest version per platform
CREATE INDEX idx_app_versions_platform_active
    ON app_versions(platform, is_active, version_code DESC);

-- Index for active versions
CREATE INDEX idx_app_versions_active
    ON app_versions(is_active, release_date DESC);

-- Comments
ALTER TABLE app_versions COMMENT = 'Stores desktop app version information for in-app updates';
