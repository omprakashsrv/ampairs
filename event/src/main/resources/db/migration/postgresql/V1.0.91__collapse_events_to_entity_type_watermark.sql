-- Collapse workspace_events to a per-(workspace_id, entity_type) watermark.
--
-- Why:
--   The previous per-row event log grew unbounded and held a unique
--   (workspace_id, sequence_number) constraint. Concurrent async handlers
--   computed `MAX(sequence_number) + 1` independently, raced on insert, and
--   one of them tripped uk_workspace_sequence (HHH000247 / 23505). Production
--   was silently dropping events on every concurrent write burst.
--
-- After this migration each workspace holds at most one row per entity type;
-- new events upsert that row (latest entity_id / event_type / sequence_number).
-- Sequence numbers come from a single global Postgres SEQUENCE — atomic by
-- definition, no race. Numbers are sparse per workspace (gaps when other
-- workspaces consume the sequence) but strictly monotonic within a workspace,
-- which is all the `?sinceSequence=N` catch-up contract requires.

TRUNCATE TABLE workspace_events;

DROP INDEX IF EXISTS uk_workspace_sequence;
DROP INDEX IF EXISTS idx_workspace_events_entity;
DROP INDEX IF EXISTS idx_workspace_events_consumed;
DROP INDEX IF EXISTS idx_workspace_events_user;
DROP INDEX IF EXISTS idx_workspace_events_device;
DROP INDEX IF EXISTS idx_workspace_events_type;

ALTER TABLE workspace_events DROP COLUMN IF EXISTS consumed;

CREATE UNIQUE INDEX uk_workspace_entity_type ON workspace_events (workspace_id, entity_type);

CREATE SEQUENCE workspace_event_seq AS BIGINT START WITH 1 INCREMENT BY 1 NO CYCLE;
