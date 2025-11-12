-- Migrate app_versions table to use S3 streaming instead of exposing URLs
-- Security improvement: No S3 URLs exposed to clients, backend streams files

-- Add new columns for S3 streaming
ALTER TABLE app_versions ADD COLUMN s3_key VARCHAR(500) NULL;
ALTER TABLE app_versions ADD COLUMN filename VARCHAR(255) NULL;

-- Migrate existing data (if any): extract filename from download_url
-- Example: https://s3.amazonaws.com/bucket/updates/macos-1.0.0.10.dmg -> updates/macos-1.0.0.10.dmg
UPDATE app_versions
SET s3_key = SUBSTRING_INDEX(download_url, '/', -2),
    filename = SUBSTRING_INDEX(download_url, '/', -1)
WHERE download_url IS NOT NULL;

-- Make new columns NOT NULL after migration
ALTER TABLE app_versions MODIFY COLUMN s3_key VARCHAR(500) NOT NULL;
ALTER TABLE app_versions MODIFY COLUMN filename VARCHAR(255) NOT NULL;

-- Drop old columns
ALTER TABLE app_versions DROP COLUMN download_url;
ALTER TABLE app_versions DROP COLUMN file_path;

-- Add comments
ALTER TABLE app_versions MODIFY COLUMN s3_key VARCHAR(500) NOT NULL COMMENT 'S3 object key for backend streaming (e.g., "updates/macos-1.0.0.10.dmg")';
ALTER TABLE app_versions MODIFY COLUMN filename VARCHAR(255) NOT NULL COMMENT 'Original filename for download attachment';
