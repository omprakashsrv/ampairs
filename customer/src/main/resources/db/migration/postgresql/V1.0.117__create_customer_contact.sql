-- customer_contact: people who order on behalf of a CRM customer account, each optionally linked to
-- an ecom storefront login (ecom_user_id). Models many users -> one customer and one user -> many
-- customers, so storefront orders resolve to (or let the buyer select) the right CRM account.
CREATE TABLE customer_contact
(
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(40)  NOT NULL UNIQUE,
    owner_id      VARCHAR(40)  NOT NULL,
    ref_id        VARCHAR(255),
    customer_id   VARCHAR(200) NOT NULL,
    ecom_user_id  VARCHAR(200),
    name          VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(255),
    role          VARCHAR(40)  NOT NULL DEFAULT 'OWNER',
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One link per (account, storefront login) within a workspace. A NULL ecom_user_id (contact with no
-- login) may repeat per customer since NULLs are distinct.
CREATE UNIQUE INDEX ux_customer_contact_cust_user ON customer_contact (owner_id, customer_id, ecom_user_id);
-- Resolve a storefront login's account(s) — for checkout selection and order ingestion.
CREATE INDEX idx_customer_contact_user ON customer_contact (owner_id, ecom_user_id);
CREATE INDEX idx_customer_contact_customer ON customer_contact (owner_id, customer_id);

COMMENT ON TABLE customer_contact IS 'People linked to a CRM customer account, optionally tied to an ecom storefront login';
