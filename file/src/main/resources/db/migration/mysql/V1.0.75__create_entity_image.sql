CREATE TABLE entity_image
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
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
    metadata          JSON,
    is_primary        TINYINT(1)    NOT NULL DEFAULT 0,
    display_order     INT           NOT NULL DEFAULT 0,
    description       VARCHAR(500),
    uploaded_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active            TINYINT(1)    NOT NULL DEFAULT 1,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated      BIGINT        NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_entity_image_uid (uid),
    INDEX idx_entity_image_entity (entity_type, entity_uid),
    INDEX idx_entity_image_workspace (owner_id),
    INDEX idx_entity_image_primary (entity_type, entity_uid, is_primary),
    INDEX idx_entity_image_checksum (checksum)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'Generic entity image store — shared by customer, product, and other modules';
