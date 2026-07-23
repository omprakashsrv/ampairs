-- Pricing Module Extension: Effective-dated price items (MySQL)
-- Description: Effective dating (Tally "Applicable From") for price history + back-dated/scheduled
--              pricing. Additive nullable columns — legacy rows (null effective_from) stay effective
--              from the beginning, so existing resolution is unchanged.
-- Dependencies: V1.0.107__create_pricing_module_tables.sql

ALTER TABLE price_list_item ADD COLUMN effective_from TIMESTAMP NULL DEFAULT NULL;
ALTER TABLE price_list_item ADD COLUMN effective_to TIMESTAMP NULL DEFAULT NULL;
