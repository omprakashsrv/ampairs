-- Recurrence is evaluated in the workspace business timezone (IANA, e.g. Asia/Kolkata) — PostgreSQL.
-- Stored on the schedule (set by the client from the workspace business locale); defaults UTC.
ALTER TABLE communication_schedule ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';
