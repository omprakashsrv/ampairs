-- MySQL counterpart of postgresql/V1.0.67.
-- PostgreSQL adds a generated tsvector column + GIN index; MySQL's nearest schema-level
-- equivalent is an InnoDB FULLTEXT index over the same columns.
--
-- KNOWN CODE GAP: EcomListedProductRepository.search uses PostgreSQL-native
-- `search_vector @@ plainto_tsquery(...)` in a native query, which does not run on MySQL.
-- A MySQL deployment needs a MATCH(...) AGAINST(...) variant of that query (tracked as a
-- follow-up in NO_MIGRATION_NEEDED.md); this index prepares the schema for it.

CREATE FULLTEXT INDEX idx_ecom_product_search
    ON ecom_listed_product (name, brand, category, subcategory);
