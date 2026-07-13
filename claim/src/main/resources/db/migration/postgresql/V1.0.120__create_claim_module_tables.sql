-- Claim Module Migration (PostgreSQL)
-- Version: 1.0.120
-- Description: Trade-scheme claims & settlement (the reimbursement layer 015 deferred).
--              Scheme *definition* tables live in pricing/spec 015 — only claims + settlements here.

CREATE TABLE scheme_claims
(
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(40)    NOT NULL UNIQUE,
    scheme_ref               VARCHAR(40)    NOT NULL,
    link_uid                 VARCHAR(40),
    brand_workspace_id       VARCHAR(40)    NOT NULL,
    distributor_workspace_id VARCHAR(40)    NOT NULL,
    period_key               VARCHAR(20),
    computed_amount          DECIMAL(19, 4) NOT NULL DEFAULT 0,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    rejection_reason         VARCHAR(500),
    created_at               TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_claim_scheme ON scheme_claims (scheme_ref);
CREATE INDEX idx_claim_brand ON scheme_claims (brand_workspace_id);
CREATE INDEX idx_claim_distributor ON scheme_claims (distributor_workspace_id);
CREATE INDEX idx_claim_status ON scheme_claims (status);

CREATE TABLE claim_settlements
(
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(40)    NOT NULL UNIQUE,
    claim_uid     VARCHAR(40)    NOT NULL,
    settled_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    reference     VARCHAR(120)   NOT NULL,
    settled_at    TIMESTAMP(6)   NOT NULL,
    created_at    TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_settlement_claim ON claim_settlements (claim_uid);
