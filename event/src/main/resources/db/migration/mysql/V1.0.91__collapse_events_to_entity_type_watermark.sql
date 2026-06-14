-- Collapse workspace_events to a per-(workspace_id, entity_type) watermark.
-- See the matching postgresql migration for context.
--
-- NOTE: the runtime database is PostgreSQL; the event listener uses
-- Postgres-specific `INSERT … ON CONFLICT … RETURNING` plus a SEQUENCE for
-- atomic sequence number generation. This MySQL migration mirrors the schema
-- shape for consistency with the per-vendor Flyway layout but is not exercised
-- at runtime.

TRUNCATE TABLE workspace_events;

ALTER TABLE workspace_events DROP INDEX uk_workspace_sequence;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_entity;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_consumed;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_user;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_device;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_type;

ALTER TABLE workspace_events DROP COLUMN consumed;

CREATE UNIQUE INDEX uk_workspace_entity_type ON workspace_events (workspace_id, entity_type);
