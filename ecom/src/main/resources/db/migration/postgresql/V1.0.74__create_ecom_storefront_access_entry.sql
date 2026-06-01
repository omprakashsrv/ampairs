CREATE TABLE ecom_storefront_access_entry (
    id         BIGSERIAL PRIMARY KEY,
    uid        VARCHAR(200)  NOT NULL,
    owner_id   VARCHAR(200),
    ref_id     VARCHAR(255),
    created_at TIMESTAMPTZ   DEFAULT NOW(),
    updated_at TIMESTAMPTZ   DEFAULT NOW(),
    storefront_id      VARCHAR(200) NOT NULL,
    identifier_type    VARCHAR(20)  NOT NULL,
    identifier_value   VARCHAR(320) NOT NULL,

    CONSTRAINT uq_ecom_storefront_access_entry_uid UNIQUE (uid),
    CONSTRAINT uq_ecom_storefront_access_entry UNIQUE (storefront_id, identifier_type, identifier_value)
);

CREATE INDEX idx_ecom_storefront_access_entry_storefront ON ecom_storefront_access_entry (storefront_id);
