-- Migrate device_session timestamp columns from TIMESTAMP (no tz) to TIMESTAMPTZ
ALTER TABLE device_session
    ALTER COLUMN last_activity TYPE TIMESTAMPTZ USING last_activity AT TIME ZONE 'UTC',
    ALTER COLUMN login_time    TYPE TIMESTAMPTZ USING login_time    AT TIME ZONE 'UTC',
    ALTER COLUMN expired_at    TYPE TIMESTAMPTZ USING expired_at    AT TIME ZONE 'UTC';
