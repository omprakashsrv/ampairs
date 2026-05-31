CREATE TABLE ecom_taxonomy_image (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(200)  NOT NULL UNIQUE,
    owner_id        VARCHAR(200),
    ref_id          VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    storefront_id   VARCHAR(200)  NOT NULL,
    taxonomy_type   VARCHAR(20)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    image_url       VARCHAR(500)  NOT NULL,
    sort_order      INT           NOT NULL DEFAULT 0,
    CONSTRAINT uq_ecom_taxonomy UNIQUE (storefront_id, taxonomy_type, name)
);

CREATE INDEX idx_ecom_taxonomy_storefront ON ecom_taxonomy_image(storefront_id, taxonomy_type);
