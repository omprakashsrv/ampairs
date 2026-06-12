-- Tax identity/configuration moved to the tax module (TaxConfiguration / /tax/v1/configurations).
-- Drop the now-unused tax columns from the business profile.
ALTER TABLE businesses DROP COLUMN IF EXISTS tax_id;
ALTER TABLE businesses DROP COLUMN IF EXISTS registration_number;
ALTER TABLE businesses DROP COLUMN IF EXISTS tax_settings;
