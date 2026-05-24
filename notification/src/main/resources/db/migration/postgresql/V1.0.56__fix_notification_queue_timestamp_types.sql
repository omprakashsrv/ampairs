-- Migrate notification_queue timestamp columns from TIMESTAMP to TIMESTAMPTZ
ALTER TABLE notification_queue
    ALTER COLUMN scheduled_at   TYPE TIMESTAMPTZ USING scheduled_at   AT TIME ZONE 'UTC',
    ALTER COLUMN last_attempt_at TYPE TIMESTAMPTZ USING last_attempt_at AT TIME ZONE 'UTC';
