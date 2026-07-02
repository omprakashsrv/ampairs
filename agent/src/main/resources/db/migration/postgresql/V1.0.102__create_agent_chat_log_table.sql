-- Agent Module — Chat Telemetry (PostgreSQL)
-- First table owned by the `agent` module. Stores opt-in assistant turns uploaded from the
-- on-device AI assistant (a user message + the assistant reply) for later analysis. Workspace-scoped
-- via owner_id (X-Workspace-ID); write-mostly, no read API yet.

CREATE TABLE agent_chat_log
(
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(200) NOT NULL UNIQUE,
    owner_id          VARCHAR(200) NOT NULL,
    ref_id            VARCHAR(255),
    user_id           VARCHAR(200) NOT NULL,
    session_id        VARCHAR(200),
    user_message      TEXT         NOT NULL,
    assistant_message TEXT         NOT NULL,
    model_id          VARCHAR(200),
    intent            VARCHAR(50),
    module_name       VARCHAR(100),
    action_type       VARCHAR(100),
    client_timestamp  TIMESTAMP(6),
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_chat_log_owner ON agent_chat_log (owner_id);
CREATE INDEX idx_agent_chat_log_session ON agent_chat_log (session_id);
