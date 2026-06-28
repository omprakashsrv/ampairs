-- Add preferred communication locale to customer (MySQL)
-- Used by the communication module for per-recipient template variant (language) selection.
ALTER TABLE customer ADD COLUMN locale VARCHAR(16);
