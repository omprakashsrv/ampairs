-- Migrate device_sessions (WebSocketSession) timestamp columns from TIMESTAMP to TIMESTAMPTZ
ALTER TABLE device_sessions
    ALTER COLUMN last_heartbeat  TYPE TIMESTAMPTZ USING last_heartbeat  AT TIME ZONE 'UTC',
    ALTER COLUMN connected_at    TYPE TIMESTAMPTZ USING connected_at    AT TIME ZONE 'UTC',
    ALTER COLUMN disconnected_at TYPE TIMESTAMPTZ USING disconnected_at AT TIME ZONE 'UTC';
