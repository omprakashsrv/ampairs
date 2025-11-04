-- Auth Module Database Migration Script (PostgreSQL)
-- Version: 1.0.15
-- Description: Add firebase_uid column to app_user table for Firebase authentication integration
-- Author: Claude Code
-- Date: 2025-10-25
-- Dependencies: V1.0.13__create_app_user_table.sql

-- =====================================================
-- Add Firebase UID Column
-- =====================================================
ALTER TABLE app_user
ADD COLUMN firebase_uid VARCHAR(128) NULL;

COMMENT ON COLUMN app_user.firebase_uid IS 'Firebase authentication UID for users who authenticate via Firebase';

-- Add index for faster Firebase UID lookups
CREATE INDEX idx_app_user_firebase_uid ON app_user(firebase_uid);
