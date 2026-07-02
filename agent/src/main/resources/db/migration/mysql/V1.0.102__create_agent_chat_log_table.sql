-- Agent Module — Chat Telemetry (MySQL)
-- First table owned by the `agent` module. Stores opt-in assistant turns uploaded from the
-- on-device AI assistant (a user message + the assistant reply) for later analysis. Workspace-scoped
-- via owner_id (X-Workspace-ID); write-mostly, no read API yet.

CREATE TABLE agent_chat_log
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uid               VARCHAR(200) NOT NULL,
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
    client_timestamp  TIMESTAMP    NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_agent_chat_log_uid (uid),
    INDEX idx_agent_chat_log_owner (owner_id),
    INDEX idx_agent_chat_log_session (session_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
