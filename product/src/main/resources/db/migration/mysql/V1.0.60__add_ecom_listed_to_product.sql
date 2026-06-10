-- MySQL counterpart of postgresql/V1.0.60.
ALTER TABLE product ADD COLUMN is_ecom_listed BOOLEAN NOT NULL DEFAULT FALSE;
