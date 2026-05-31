CREATE TABLE ecom_storefront (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(200) NOT NULL UNIQUE,
    owner_id    VARCHAR(200),
    ref_id      VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(50)  NOT NULL UNIQUE,
    description TEXT,
    logo_url    VARCHAR(500),
    banner_url  VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMPTZ,
    unpublished_at  TIMESTAMPTZ
);

CREATE INDEX idx_ecom_storefront_owner  ON ecom_storefront(owner_id);
CREATE INDEX idx_ecom_storefront_status ON ecom_storefront(status);
