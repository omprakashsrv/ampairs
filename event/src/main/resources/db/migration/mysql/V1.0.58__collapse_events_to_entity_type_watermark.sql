-- Collapse workspace_events to a per-(workspace_id, entity_type) watermark.
-- See the matching postgresql migration for context.

TRUNCATE TABLE workspace_events;

ALTER TABLE workspace_events DROP INDEX uk_workspace_sequence;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_entity;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_consumed;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_user;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_device;
ALTER TABLE workspace_events DROP INDEX idx_workspace_events_type;

ALTER TABLE workspace_events DROP COLUMN consumed;

CREATE UNIQUE INDEX uk_workspace_entity_type ON workspace_events (workspace_id, entity_type);

CREATE TABLE workspace_event_sequence
(
    workspace_id VARCHAR(40) NOT NULL,
    current_seq  BIGINT      NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Atomic per-workspace event sequence counter';
