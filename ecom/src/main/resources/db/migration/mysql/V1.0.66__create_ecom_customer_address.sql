-- MySQL counterpart of postgresql/V1.0.66.
CREATE TABLE ecom_customer_address (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    uid             VARCHAR(200) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    customer_id     VARCHAR(200) NOT NULL,
    label           VARCHAR(50),
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pin_code        VARCHAR(20)  NOT NULL,
    country         VARCHAR(10)  NOT NULL DEFAULT 'IN',
    phone           VARCHAR(20),
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_ecom_customer_address_uid (uid),
    INDEX idx_ecom_customer_address_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
