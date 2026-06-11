-- MySQL counterpart of postgresql/V1.0.52.
-- (MySQL 8.0 has no ADD COLUMN IF NOT EXISTS; safe here because the MySQL vendor history
-- never created these columns.)
ALTER TABLE unit
    ADD COLUMN category VARCHAR(50),
    ADD COLUMN description TEXT;
