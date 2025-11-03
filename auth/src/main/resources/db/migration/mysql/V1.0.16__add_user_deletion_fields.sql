-- Migration: Add account deletion fields to app_user table
-- Description: Adds soft delete support with 30-day grace period for user account deletion

ALTER TABLE app_user
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether this user account has been marked for deletion',
    ADD COLUMN deleted_at TIMESTAMP NULL COMMENT 'When the account deletion was requested',
    ADD COLUMN deletion_scheduled_for TIMESTAMP NULL COMMENT 'When the account will be permanently deleted (30 days after deletion request)',
    ADD COLUMN deletion_reason VARCHAR(500) NULL COMMENT 'Reason provided for account deletion';

-- Create index for querying deleted users and scheduled deletions
CREATE INDEX idx_app_user_deleted ON app_user(deleted);
CREATE INDEX idx_app_user_deletion_scheduled ON app_user(deletion_scheduled_for) WHERE deletion_scheduled_for IS NOT NULL;
