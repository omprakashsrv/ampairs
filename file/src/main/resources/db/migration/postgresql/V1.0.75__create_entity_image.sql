CREATE TABLE entity_image
(
    id                BIGSERIAL     NOT NULL,
    uid               VARCHAR(200)  NOT NULL,
    owner_id          VARCHAR(200)  NOT NULL,
    ref_id            VARCHAR(255),
    entity_type       VARCHAR(50)   NOT NULL,
    entity_uid        VARCHAR(200)  NOT NULL,
    workspace_slug    VARCHAR(100)  NOT NULL,
    original_filename VARCHAR(500)  NOT NULL DEFAULT '',
    file_extension    VARCHAR(20)   NOT NULL DEFAULT '',
    content_type      VARCHAR(100)  NOT NULL DEFAULT '',
    file_size         BIGINT        NOT NULL DEFAULT 0,
    storage_path      VARCHAR(1000) NOT NULL DEFAULT '',
    storage_url       VARCHAR(1000),
    checksum          VARCHAR(64)   NOT NULL DEFAULT '',
    metadata          JSONB,
    is_primary        BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order     INT           NOT NULL DEFAULT 0,
    description       VARCHAR(500),
    uploaded_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_updated      BIGINT        NOT NULL DEFAULT 0,

    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_entity_image_uid ON entity_image (uid);
CREATE INDEX idx_entity_image_entity ON entity_image (entity_type, entity_uid);
CREATE INDEX idx_entity_image_workspace ON entity_image (owner_id);
CREATE INDEX idx_entity_image_primary ON entity_image (entity_type, entity_uid, is_primary);
CREATE INDEX idx_entity_image_checksum ON entity_image (checksum);
