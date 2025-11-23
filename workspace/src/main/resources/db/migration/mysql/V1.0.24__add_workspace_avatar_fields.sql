-- Migration: Add avatar thumbnail and updated_at fields to workspaces table
-- Description: Adds support for workspace avatars with full size and thumbnail URLs

ALTER TABLE workspaces
    ADD COLUMN avatar_thumbnail_url VARCHAR(500) NULL COMMENT 'URL to the thumbnail avatar (256x256)',
    ADD COLUMN avatar_updated_at TIMESTAMP NULL COMMENT 'When the avatar was last updated';
