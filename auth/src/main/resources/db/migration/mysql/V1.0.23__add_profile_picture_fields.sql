-- Migration: Add profile picture fields to app_user table
-- Description: Adds support for user profile pictures with full size and thumbnail URLs

ALTER TABLE app_user
    ADD COLUMN profile_picture_url VARCHAR(500) NULL COMMENT 'URL to the full-size profile picture',
    ADD COLUMN profile_picture_thumbnail_url VARCHAR(500) NULL COMMENT 'URL to the thumbnail profile picture (256x256)',
    ADD COLUMN profile_picture_updated_at TIMESTAMP NULL COMMENT 'When the profile picture was last updated';
