CREATE TABLE ecom_customer_address (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(200) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    customer_id     VARCHAR(200) NOT NULL,
    label           VARCHAR(50),
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pin_code        VARCHAR(20)  NOT NULL,
    country         VARCHAR(10)  NOT NULL DEFAULT 'IN',
    phone           VARCHAR(20),
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ecom_customer_address_customer ON ecom_customer_address(customer_id);
