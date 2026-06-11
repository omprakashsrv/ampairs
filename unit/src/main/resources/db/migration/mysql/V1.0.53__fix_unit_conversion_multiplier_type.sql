-- MySQL counterpart of postgresql/V1.0.53: unit_conversion.multiplier DOUBLE → DECIMAL(20,6).
ALTER TABLE unit_conversion
    MODIFY COLUMN multiplier DECIMAL(20,6) NOT NULL DEFAULT 1.0;
