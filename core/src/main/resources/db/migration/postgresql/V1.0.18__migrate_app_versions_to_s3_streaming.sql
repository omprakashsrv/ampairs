-- Migrate app_versions table to use S3 streaming instead of exposing URLs
-- Security improvement: No S3 URLs exposed to clients, backend streams files

-- Add new columns for S3 streaming
ALTER TABLE app_versions ADD COLUMN s3_key VARCHAR(500);
ALTER TABLE app_versions ADD COLUMN filename VARCHAR(255);

-- Migrate existing data (if any): extract filename from download_url
-- Example: https://s3.amazonaws.com/bucket/updates/macos-1.0.0.10.dmg -> updates/macos-1.0.0.10.dmg
UPDATE app_versions
SET s3_key = SUBSTRING(download_url FROM 'updates/.*$'),
    filename = SUBSTRING(download_url FROM '[^/]+$')
WHERE download_url IS NOT NULL;

-- Make new columns NOT NULL after migration
ALTER TABLE app_versions ALTER COLUMN s3_key SET NOT NULL;
ALTER TABLE app_versions ALTER COLUMN filename SET NOT NULL;

-- Drop old columns
ALTER TABLE app_versions DROP COLUMN download_url;
ALTER TABLE app_versions DROP COLUMN file_path;

-- Add comment
COMMENT ON COLUMN app_versions.s3_key IS 'S3 object key for backend streaming (e.g., "updates/macos-1.0.0.10.dmg")';
COMMENT ON COLUMN app_versions.filename IS 'Original filename for download attachment';
