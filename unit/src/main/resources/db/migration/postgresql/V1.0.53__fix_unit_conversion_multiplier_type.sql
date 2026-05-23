-- Unit Module Migration (PostgreSQL)
-- Version: 1.0.53
-- Description: Fix unit_conversion.multiplier column type from DOUBLE to NUMERIC(20,6)

ALTER TABLE unit_conversion
    ALTER COLUMN multiplier TYPE NUMERIC(20, 6) USING multiplier::NUMERIC(20, 6);
