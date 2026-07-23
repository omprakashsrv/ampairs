-- Supplier Module Database Migration Script (MySQL)
-- Description: Create tenant-scoped supplier master data table (buy-side counterparty)
-- Mirrors the customer master on the sell side.

CREATE TABLE supplier
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    uid                VARCHAR(40)  NOT NULL,
    owner_id           VARCHAR(40)  NOT NULL,
    ref_id             VARCHAR(255),
    country_code       INT          NOT NULL,
    name               VARCHAR(255) NOT NULL,
    supplier_type      VARCHAR(100),
    supplier_group     VARCHAR(100),
    phone              VARCHAR(20),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    landline           VARCHAR(12),
    email              VARCHAR(255),
    gst_number         VARCHAR(15),
    pan_number         VARCHAR(10),
    credit_limit       DOUBLE,
    credit_days        INT,
    outstanding_amount DOUBLE,
    address            VARCHAR(255),
    street             VARCHAR(255),
    street2            VARCHAR(255),
    city               VARCHAR(255),
    pincode            VARCHAR(10),
    state              VARCHAR(20),
    country            VARCHAR(20),
    location           POINT,
    billing_address    JSON,
    shipping_address   JSON,
    attributes         JSON,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_supplier_uid (uid),
    INDEX idx_supplier_owner_name (owner_id, name),
    INDEX idx_supplier_owner_phone (owner_id, phone),
    INDEX idx_supplier_owner_email (owner_id, email),
    INDEX idx_supplier_status (status),
    INDEX idx_supplier_gst (gst_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT ='Workspace supplier master records (buy-side counterparty) with billing and shipping metadata';
