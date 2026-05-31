CREATE TABLE ecom_order (
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(200) NOT NULL UNIQUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ecom_order_ref          VARCHAR(50)  NOT NULL UNIQUE,
    storefront_id           VARCHAR(200) NOT NULL,
    workspace_id            VARCHAR(200) NOT NULL,
    customer_id             VARCHAR(200) NOT NULL,
    customer_name           VARCHAR(255) NOT NULL,
    customer_email          VARCHAR(320) NOT NULL,
    customer_phone          VARCHAR(20),
    delivery_address        JSONB        NOT NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PLACED',
    management_order_ref    VARCHAR(255),
    subtotal                NUMERIC(19,4) NOT NULL,
    total_amount            NUMERIC(19,4) NOT NULL,
    notes                   TEXT,
    placed_at               TIMESTAMPTZ  NOT NULL,
    confirmed_at            TIMESTAMPTZ,
    merchant_reviewed_at    TIMESTAMPTZ
);

CREATE INDEX idx_ecom_order_ref        ON ecom_order(ecom_order_ref);
CREATE INDEX idx_ecom_order_storefront ON ecom_order(storefront_id);
CREATE INDEX idx_ecom_order_customer   ON ecom_order(customer_id);
CREATE INDEX idx_ecom_order_workspace  ON ecom_order(workspace_id);
CREATE INDEX idx_ecom_order_status     ON ecom_order(status);

CREATE TABLE ecom_order_line_item (
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(200) NOT NULL UNIQUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ecom_order_id           VARCHAR(200) NOT NULL,
    listed_product_id       VARCHAR(200) NOT NULL,
    management_product_id   VARCHAR(200) NOT NULL,
    product_name            VARCHAR(255) NOT NULL,
    unit_price              NUMERIC(19,4) NOT NULL,
    quantity_ordered        INT          NOT NULL,
    quantity_confirmed      INT,
    line_total              NUMERIC(19,4) NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ORDERED',
    shipment_group          VARCHAR(100)
);

CREATE INDEX idx_ecom_order_line_item_order ON ecom_order_line_item(ecom_order_id);
