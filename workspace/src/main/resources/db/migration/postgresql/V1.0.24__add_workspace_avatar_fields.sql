-- Migration: Add avatar thumbnail and updated_at fields to workspaces table
-- Description: Adds support for workspace avatars with full size and thumbnail URLs

ALTER TABLE workspaces
    ADD COLUMN avatar_thumbnail_url VARCHAR(500) NULL,
    ADD COLUMN avatar_updated_at TIMESTAMP NULL;

COMMENT ON COLUMN workspaces.avatar_thumbnail_url IS 'URL to the thumbnail avatar (256x256)';
COMMENT ON COLUMN workspaces.avatar_updated_at IS 'When the avatar was last updated';
