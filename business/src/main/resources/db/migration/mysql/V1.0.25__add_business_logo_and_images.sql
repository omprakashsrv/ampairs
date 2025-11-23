-- Add logo fields to businesses table
ALTER TABLE businesses
    ADD COLUMN logo_url VARCHAR(500) NULL AFTER website,
    ADD COLUMN logo_thumbnail_url VARCHAR(500) NULL AFTER logo_url,
    ADD COLUMN logo_updated_at TIMESTAMP NULL AFTER logo_thumbnail_url;

-- Create business_images table for gallery images
CREATE TABLE business_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    ref_id VARCHAR(255) NULL,
    business_id VARCHAR(36) NOT NULL,
    image_type VARCHAR(30) NOT NULL DEFAULT 'GALLERY',
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500) NULL,
    title VARCHAR(255) NULL,
    description VARCHAR(1000) NULL,
    alt_text VARCHAR(255) NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    original_filename VARCHAR(255) NULL,
    file_size BIGINT NULL,
    width INT NULL,
    height INT NULL,
    content_type VARCHAR(50) NULL,
    uploaded_by VARCHAR(36) NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_images_uid (uid),
    INDEX idx_business_image_business_id (business_id),
    INDEX idx_business_image_owner_id (owner_id),
    INDEX idx_business_image_type (image_type),
    INDEX idx_business_image_display_order (display_order),
    INDEX idx_business_image_is_primary (is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment for documentation
ALTER TABLE business_images COMMENT = 'Business gallery images for showcasing business photos';
