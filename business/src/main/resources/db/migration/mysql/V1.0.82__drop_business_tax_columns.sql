-- Tax identity/configuration moved to the tax module (TaxConfiguration / /tax/v1/configurations).
-- Drop the now-unused tax columns from the business profile.
-- (MySQL 8.0 has no DROP COLUMN IF EXISTS; these columns always exist from V1.0.12.)
ALTER TABLE businesses DROP COLUMN tax_id;
ALTER TABLE businesses DROP COLUMN registration_number;
ALTER TABLE businesses DROP COLUMN tax_settings;
