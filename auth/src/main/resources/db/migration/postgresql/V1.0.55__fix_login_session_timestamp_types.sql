-- Migrate login_session timestamp columns from TIMESTAMP (no tz) to TIMESTAMPTZ
ALTER TABLE login_session
    ALTER COLUMN expiry_time  TYPE TIMESTAMPTZ USING expiry_time  AT TIME ZONE 'UTC',
    ALTER COLUMN verified_at  TYPE TIMESTAMPTZ USING verified_at  AT TIME ZONE 'UTC';
