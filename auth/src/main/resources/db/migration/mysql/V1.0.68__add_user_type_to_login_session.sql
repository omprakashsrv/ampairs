-- MySQL counterpart of postgresql/V1.0.68.
ALTER TABLE login_session ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'MERCHANT_USER';
