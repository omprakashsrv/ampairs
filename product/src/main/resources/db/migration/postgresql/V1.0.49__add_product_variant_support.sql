-- Migration: Add Product Variant Support
-- Version: V1.0.22
-- Description: Adds product classification and variant support to product module

-- Add product classification columns to product table
ALTER TABLE product
ADD COLUMN product_type VARCHAR(50),
ADD COLUMN service_type VARCHAR(50),
ADD COLUMN has_variants BOOLEAN DEFAULT FALSE NOT NULL;

-- Add index for product_type filtering
CREATE INDEX idx_product_product_type ON product(product_type);
CREATE INDEX idx_product_has_variants ON product(has_variants);

-- Create product_variant table
CREATE TABLE product_variant (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(255) NOT NULL UNIQUE,
    owner_id VARCHAR(255) NOT NULL,
    ref_id VARCHAR(255),
    workspace_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    variant_name VARCHAR(255) NOT NULL,

    -- Variant attributes (flexible key-value pairs)
    attribute_1_name VARCHAR(100),
    attribute_1_value VARCHAR(255),
    attribute_2_name VARCHAR(100),
    attribute_2_value VARCHAR(255),
    attribute_3_name VARCHAR(100),
    attribute_3_value VARCHAR(255),

    -- Pricing (can override product-level pricing)
    mrp DECIMAL(15,2),
    dp DECIMAL(15,2),
    selling_price DECIMAL(15,2),

    -- Stock management
    stock_quantity DECIMAL(15,3) DEFAULT 0 NOT NULL,
    low_stock_alert DECIMAL(15,3),

    -- Status
    active BOOLEAN DEFAULT TRUE NOT NULL,
    synced BOOLEAN DEFAULT FALSE NOT NULL,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to product (via uid, not id)
    CONSTRAINT fk_product_variant_product_uid FOREIGN KEY (product_id)
        REFERENCES product(uid) ON DELETE CASCADE
);

-- Create indices for product_variant
CREATE INDEX idx_product_variant_owner_id ON product_variant(owner_id);
CREATE INDEX idx_product_variant_workspace_id ON product_variant(workspace_id);
CREATE INDEX idx_product_variant_product_id ON product_variant(product_id);
CREATE INDEX idx_product_variant_sku ON product_variant(sku);
CREATE INDEX idx_product_variant_active ON product_variant(active);
CREATE INDEX idx_product_variant_updated_at ON product_variant(updated_at);

-- Create searchable variant attributes table (for advanced queries)
CREATE TABLE variant_attribute (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(255) NOT NULL UNIQUE,
    owner_id VARCHAR(255) NOT NULL,
    ref_id VARCHAR(255),
    workspace_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to product (via uid)
    CONSTRAINT fk_variant_attribute_product_uid FOREIGN KEY (product_id)
        REFERENCES product(uid) ON DELETE CASCADE,
    CONSTRAINT uq_variant_attribute UNIQUE (owner_id, product_id, attribute_name, attribute_value)
);

-- Create indices for variant_attribute
CREATE INDEX idx_variant_attribute_owner_id ON variant_attribute(owner_id);
CREATE INDEX idx_variant_attribute_workspace_id ON variant_attribute(workspace_id);
CREATE INDEX idx_variant_attribute_product_id ON variant_attribute(product_id);
CREATE INDEX idx_variant_attribute_name ON variant_attribute(attribute_name);
CREATE INDEX idx_variant_attribute_value ON variant_attribute(attribute_value);

-- Update existing products to have default product_type
UPDATE product SET product_type = 'RETAIL' WHERE product_type IS NULL;
