-- Claim Module Migration (MySQL)
-- Version: 1.0.120
-- Description: Trade-scheme claims & settlement (the reimbursement layer 015 deferred).

CREATE TABLE scheme_claims
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    uid                      VARCHAR(40)    NOT NULL,
    scheme_ref               VARCHAR(40)    NOT NULL,
    link_uid                 VARCHAR(40),
    brand_workspace_id       VARCHAR(40)    NOT NULL,
    distributor_workspace_id VARCHAR(40)    NOT NULL,
    period_key               VARCHAR(20),
    computed_amount          DECIMAL(19, 4) NOT NULL DEFAULT 0,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    rejection_reason         VARCHAR(500),
    created_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_claim_uid (uid),
    INDEX idx_claim_scheme (scheme_ref),
    INDEX idx_claim_brand (brand_workspace_id),
    INDEX idx_claim_distributor (distributor_workspace_id),
    INDEX idx_claim_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE claim_settlements
(
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40)    NOT NULL,
    claim_uid      VARCHAR(40)    NOT NULL,
    settled_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    reference      VARCHAR(120)   NOT NULL,
    settled_at     TIMESTAMP      NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_settlement_uid (uid),
    INDEX idx_settlement_claim (claim_uid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
