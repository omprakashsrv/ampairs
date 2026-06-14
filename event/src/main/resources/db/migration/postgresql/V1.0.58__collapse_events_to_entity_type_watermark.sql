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
-- Sequence numbers are vended atomically from workspace_event_sequence.

TRUNCATE TABLE workspace_events;

DROP INDEX IF EXISTS uk_workspace_sequence;
DROP INDEX IF EXISTS idx_workspace_events_entity;
DROP INDEX IF EXISTS idx_workspace_events_consumed;
DROP INDEX IF EXISTS idx_workspace_events_user;
DROP INDEX IF EXISTS idx_workspace_events_device;
DROP INDEX IF EXISTS idx_workspace_events_type;

ALTER TABLE workspace_events DROP COLUMN IF EXISTS consumed;

CREATE UNIQUE INDEX uk_workspace_entity_type ON workspace_events (workspace_id, entity_type);

CREATE TABLE workspace_event_sequence
(
    workspace_id VARCHAR(40)  PRIMARY KEY,
    current_seq  BIGINT       NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE workspace_event_sequence IS 'Atomic per-workspace event sequence counter; one row per workspace, incremented by UPDATE … RETURNING.';
