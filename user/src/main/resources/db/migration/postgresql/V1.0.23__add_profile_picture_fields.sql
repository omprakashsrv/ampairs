-- Migration: Add profile picture fields to app_user table
-- Description: Adds support for user profile pictures with full size and thumbnail URLs

ALTER TABLE app_user
    ADD COLUMN profile_picture_url VARCHAR(500) NULL,
    ADD COLUMN profile_picture_thumbnail_url VARCHAR(500) NULL,
    ADD COLUMN profile_picture_updated_at TIMESTAMP NULL;

COMMENT ON COLUMN app_user.profile_picture_url IS 'URL to the full-size profile picture (max 512x512)';
COMMENT ON COLUMN app_user.profile_picture_thumbnail_url IS 'URL to the thumbnail profile picture (256x256)';
COMMENT ON COLUMN app_user.profile_picture_updated_at IS 'When the profile picture was last updated';
