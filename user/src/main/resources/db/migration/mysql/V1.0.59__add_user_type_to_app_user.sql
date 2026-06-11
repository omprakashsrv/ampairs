-- MySQL counterpart of postgresql/V1.0.59.
ALTER TABLE app_user ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'MERCHANT_USER';
