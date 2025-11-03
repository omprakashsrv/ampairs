-- Migration: Add account deletion fields to app_user table
-- Description: Adds soft delete support with 30-day grace period for user account deletion

ALTER TABLE app_user
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deletion_scheduled_for TIMESTAMP NULL,
    ADD COLUMN deletion_reason VARCHAR(500) NULL;

COMMENT ON COLUMN app_user.deleted IS 'Whether this user account has been marked for deletion';
COMMENT ON COLUMN app_user.deleted_at IS 'When the account deletion was requested';
COMMENT ON COLUMN app_user.deletion_scheduled_for IS 'When the account will be permanently deleted (30 days after deletion request)';
COMMENT ON COLUMN app_user.deletion_reason IS 'Reason provided for account deletion';

-- Create index for querying deleted users and scheduled deletions
CREATE INDEX idx_app_user_deleted ON app_user(deleted);
CREATE INDEX idx_app_user_deletion_scheduled ON app_user(deletion_scheduled_for) WHERE deletion_scheduled_for IS NOT NULL;
