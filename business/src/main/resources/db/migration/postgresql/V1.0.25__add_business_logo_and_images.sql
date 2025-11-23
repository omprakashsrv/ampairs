-- Add logo fields to businesses table
ALTER TABLE businesses
    ADD COLUMN logo_url VARCHAR(500),
    ADD COLUMN logo_thumbnail_url VARCHAR(500),
    ADD COLUMN logo_updated_at TIMESTAMP;

-- Create business_images table for gallery images
CREATE TABLE business_images (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    ref_id VARCHAR(255),
    business_id VARCHAR(36) NOT NULL,
    image_type VARCHAR(30) NOT NULL DEFAULT 'GALLERY',
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    title VARCHAR(255),
    description VARCHAR(1000),
    alt_text VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    original_filename VARCHAR(255),
    file_size BIGINT,
    width INT,
    height INT,
    content_type VARCHAR(50),
    uploaded_by VARCHAR(36),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_business_images_uid UNIQUE (uid)
);

-- Create indexes
CREATE INDEX idx_business_image_business_id ON business_images (business_id);
CREATE INDEX idx_business_image_owner_id ON business_images (owner_id);
CREATE INDEX idx_business_image_type ON business_images (image_type);
CREATE INDEX idx_business_image_display_order ON business_images (display_order);
CREATE INDEX idx_business_image_is_primary ON business_images (is_primary);

-- Add comment for documentation
COMMENT ON TABLE business_images IS 'Business gallery images for showcasing business photos';
